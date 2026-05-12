package com.rts.modules.auth.application;

import com.rts.infrastructure.mail.EmailPort;
import com.rts.modules.auth.domain.PasswordResetToken;
import com.rts.modules.auth.domain.User;
import com.rts.modules.auth.persistence.PasswordResetTokenRepository;
import com.rts.modules.auth.persistence.UserRepository;
import com.rts.shared.exception.ResourceNotFoundException;
import com.rts.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailPort emailPort;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(tokenRepository, userRepository, passwordEncoder, emailPort, "http://localhost:8080");
    }

    @Test
    void requestPasswordResetShouldSendResetEmail() {
        User user = buildUser("user-1", "testuser", "test@rts.com");

        when(userRepository.findByEmail("test@rts.com")).thenReturn(Optional.of(user));
        when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(i -> i.getArgument(0));

        service.requestPasswordReset("test@rts.com");

        verify(tokenRepository).deleteUnusedByUserId("user-1");
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailPort).send(eq("test@rts.com"), contains("Password Reset"), contains("reset-password"));
    }

    @Test
    void requestPasswordResetShouldNotRevealNonExistentEmail() {
        when(userRepository.findByEmail("unknown@rts.com")).thenReturn(Optional.empty());

        service.requestPasswordReset("unknown@rts.com");

        verify(tokenRepository, never()).save(any());
        verify(emailPort, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void requestPasswordResetShouldSkipDeletedUser() {
        User user = buildUser("user-1", "deleted", "deleted@rts.com");
        user.setDeleted(true);

        when(userRepository.findByEmail("deleted@rts.com")).thenReturn(Optional.of(user));

        service.requestPasswordReset("deleted@rts.com");

        verify(tokenRepository, never()).save(any());
        verify(emailPort, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void resetPasswordShouldUpdatePasswordAndMarkTokenUsed() {
        User user = buildUser("user-1", "testuser", "test@rts.com");

        PasswordResetToken token = new PasswordResetToken();
        token.setUserId("user-1");
        token.setToken("valid-token");
        token.setExpiresAt(LocalDateTime.now().plusMinutes(15));

        when(tokenRepository.findByTokenAndUsedFalse("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-password");

        service.resetPassword("valid-token", "newPassword123", "newPassword123");

        assertThat(user.getPassword()).isEqualTo("encoded-password");
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
        verify(emailPort).send(eq("test@rts.com"), contains("Password Has Been Reset"), anyString());
    }

    @Test
    void resetPasswordShouldThrowWhenPasswordsMismatch() {
        assertThatThrownBy(() -> service.resetPassword("token", "pass1", "pass2"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Password and confirm password do not match");
    }

    @Test
    void resetPasswordShouldThrowWhenTokenInvalid() {
        when(tokenRepository.findByTokenAndUsedFalse("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("bad-token", "pass", "pass"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resetPasswordShouldThrowWhenTokenExpired() {
        PasswordResetToken token = new PasswordResetToken();
        token.setUserId("user-1");
        token.setToken("expired");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(5));

        when(tokenRepository.findByTokenAndUsedFalse("expired")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword("expired", "pass", "pass"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("expired");
    }

    private User buildUser(String id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword("old-hashed");
        return user;
    }
}
