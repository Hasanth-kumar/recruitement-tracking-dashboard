package com.rts.modules.auth.application;

import com.rts.infrastructure.mail.EmailPort;
import com.rts.modules.auth.domain.EmailVerificationToken;
import com.rts.modules.auth.domain.User;
import com.rts.modules.auth.persistence.EmailVerificationTokenRepository;
import com.rts.modules.auth.persistence.UserRepository;
import com.rts.shared.exception.ConflictException;
import com.rts.shared.exception.ResourceNotFoundException;
import com.rts.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailPort emailPort;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(tokenRepository, userRepository, emailPort, "http://localhost:8080");
    }

    @Test
    void initiateEmailChangeShouldSendVerificationEmail() {
        User user = buildUser("user-1", "testuser", "old@rts.com");

        when(userRepository.findByEmail("new@rts.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        service.initiateEmailChange(user, "new@rts.com");

        verify(tokenRepository).deleteUnconfirmedByUserId("user-1");
        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(emailPort).send(eq("new@rts.com"), contains("Verify"), contains("verify-email"));
        assertThat(user.getPendingEmail()).isEqualTo("new@rts.com");
    }

    @Test
    void initiateEmailChangeShouldThrowWhenSameEmail() {
        User user = buildUser("user-1", "testuser", "same@rts.com");

        assertThatThrownBy(() -> service.initiateEmailChange(user, "same@rts.com"))
                .isInstanceOf(ValidationException.class)
                .hasMessage("New email is the same as current email");
    }

    @Test
    void initiateEmailChangeShouldThrowWhenEmailInUse() {
        User user = buildUser("user-1", "testuser", "old@rts.com");
        User existingUser = buildUser("user-2", "other", "taken@rts.com");

        when(userRepository.findByEmail("taken@rts.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> service.initiateEmailChange(user, "taken@rts.com"))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email already in use by another account");
    }

    @Test
    void verifyEmailShouldUpdateUserEmail() {
        User user = buildUser("user-1", "testuser", "old@rts.com");
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId("user-1");
        token.setToken("valid-token");
        token.setNewEmail("new@rts.com");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(tokenRepository.findByTokenAndConfirmedFalse("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@rts.com")).thenReturn(Optional.empty());

        service.verifyEmail("valid-token");

        assertThat(user.getEmail()).isEqualTo("new@rts.com");
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getPendingEmail()).isNull();
        assertThat(token.isConfirmed()).isTrue();
        verify(userRepository).save(user);
        verify(tokenRepository).save(token);
    }

    @Test
    void verifyEmailShouldThrowWhenTokenInvalid() {
        when(tokenRepository.findByTokenAndConfirmedFalse("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyEmail("bad-token"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void verifyEmailShouldThrowWhenTokenExpired() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUserId("user-1");
        token.setToken("expired-token");
        token.setNewEmail("new@rts.com");
        token.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(tokenRepository.findByTokenAndConfirmedFalse("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verifyEmail("expired-token"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void resendVerificationShouldThrowWhenNoPendingEmail() {
        User user = buildUser("user-1", "testuser", "current@rts.com");

        assertThatThrownBy(() -> service.resendVerification(user))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("No pending email");
    }

    @Test
    void resendVerificationShouldSendNewToken() {
        User user = buildUser("user-1", "testuser", "current@rts.com");
        user.setPendingEmail("pending@rts.com");

        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(i -> i.getArgument(0));

        service.resendVerification(user);

        verify(tokenRepository).deleteUnconfirmedByUserId("user-1");
        verify(tokenRepository).save(any(EmailVerificationToken.class));
        verify(emailPort).send(eq("pending@rts.com"), anyString(), anyString());
    }

    private User buildUser(String id, String username, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        return user;
    }
}
