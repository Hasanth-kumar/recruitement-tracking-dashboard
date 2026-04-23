package com.rts.modules.auth.application;

import com.rts.modules.auth.api.dto.LoginRequest;
import com.rts.modules.auth.api.dto.LoginResponse;
import com.rts.modules.auth.domain.Role;
import com.rts.modules.auth.domain.User;
import com.rts.modules.auth.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthService authService;

    @Test
    void loginShouldReturnUserInfoForValidCredentials() {
        authService = new AuthService(userRepository, passwordEncoder);
        LoginRequest request = new LoginRequest("recruiter", "password");

        User user = new User();
        user.setId("user-1");
        user.setUsername("recruiter");
        user.setEmail("recruiter@rts.com");
        user.setPassword("hashed-password");
        user.setRole(Role.RECRUITER);

        when(userRepository.findByUsernameOrEmail("recruiter", "recruiter")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashed-password")).thenReturn(true);

        LoginResponse response = authService.login(request);

        assertThat(response.user().id()).isEqualTo("user-1");
        assertThat(response.user().username()).isEqualTo("recruiter");
        assertThat(response.user().email()).isEqualTo("recruiter@rts.com");
        assertThat(response.user().role()).isEqualTo("RECRUITER");
    }

    @Test
    void loginShouldThrowWhenUserIsNotFound() {
        authService = new AuthService(userRepository, passwordEncoder);
        LoginRequest request = new LoginRequest("unknown", "password");

        when(userRepository.findByUsernameOrEmail("unknown", "unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void loginShouldThrowWhenPasswordIsInvalid() {
        authService = new AuthService(userRepository, passwordEncoder);
        LoginRequest request = new LoginRequest("recruiter", "wrong-password");

        User user = new User();
        user.setUsername("recruiter");
        user.setPassword("hashed-password");

        when(userRepository.findByUsernameOrEmail("recruiter", "recruiter")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid username or password");
    }
}
