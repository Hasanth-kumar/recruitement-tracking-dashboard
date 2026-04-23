package com.rts.modules.auth.application;

import com.rts.modules.auth.api.dto.RegisterUserRequest;
import com.rts.modules.auth.domain.Role;
import com.rts.modules.auth.domain.User;
import com.rts.modules.auth.persistence.UserRepository;
import com.rts.shared.exception.ConflictException;
import com.rts.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(userRepository, passwordEncoder);
    }

    @Test
    void registerShouldCreateRecruiterUser() {
        RegisterUserRequest request = new RegisterUserRequest(
                "newuser",
                "newuser@rts.com",
                "NewPassword1",
                "NewPassword1"
        );
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@rts.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("NewPassword1")).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = registrationService.register(request);

        assertThat(created.getUsername()).isEqualTo("newuser");
        assertThat(created.getEmail()).isEqualTo("newuser@rts.com");
        assertThat(created.getPassword()).isEqualTo("encoded-pass");
        assertThat(created.getRole()).isEqualTo(Role.RECRUITER);
    }

    @Test
    void registerShouldFailWhenUsernameAlreadyExists() {
        RegisterUserRequest request = new RegisterUserRequest(
                "existing",
                "newuser@rts.com",
                "NewPassword1",
                "NewPassword1"
        );
        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Username already in use");
    }

    @Test
    void registerShouldFailWhenEmailAlreadyExists() {
        RegisterUserRequest request = new RegisterUserRequest(
                "newuser",
                "existing@rts.com",
                "NewPassword1",
                "NewPassword1"
        );
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@rts.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Email already in use");
    }

    @Test
    void registerShouldFailWhenPasswordsDoNotMatch() {
        RegisterUserRequest request = new RegisterUserRequest(
                "newuser",
                "newuser@rts.com",
                "NewPassword1",
                "DifferentPassword1"
        );
        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("newuser@rts.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.register(request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Password and confirm password do not match");
    }
}
