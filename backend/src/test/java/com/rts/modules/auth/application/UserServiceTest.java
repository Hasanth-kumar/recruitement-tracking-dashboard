package com.rts.modules.auth.application;

import com.rts.modules.auth.api.dto.UpdateUserProfileRequest;
import com.rts.modules.auth.domain.Role;
import com.rts.modules.auth.domain.User;
import com.rts.modules.auth.persistence.UserRepository;
import com.rts.shared.exception.ResourceNotFoundException;
import com.rts.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(authService, userRepository, passwordEncoder);
    }

    @Test
    void getProfileShouldReturnAuthenticatedUser() {
        User user = buildUser();
        when(authService.getAuthenticatedUser(authentication)).thenReturn(user);

        User result = userService.getProfile(authentication);

        assertThat(result).isSameAs(user);
    }

    @Test
    void updateProfileShouldUpdateBasicFieldsWithoutPasswordChange() {
        User user = buildUser();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                "updated-user",
                "updated@rts.com",
                null,
                null,
                null
        );

        when(authService.getAuthenticatedUser(authentication)).thenReturn(user);
        when(userRepository.findByUsername("updated-user")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("updated@rts.com")).thenReturn(Optional.empty());
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.updateProfile(authentication, request);

        assertThat(updated.getUsername()).isEqualTo("updated-user");
        assertThat(updated.getEmail()).isEqualTo("updated@rts.com");
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void updateProfileShouldFailWhenCurrentPasswordIsWrong() {
        User user = buildUser();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                null,
                null,
                "wrong",
                "NewPassword1",
                "NewPassword1"
        );

        when(authService.getAuthenticatedUser(authentication)).thenReturn(user);
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        assertThatThrownBy(() -> userService.updateProfile(authentication, request))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Current password is incorrect");
    }

    @Test
    void updateProfileShouldChangePasswordWhenValid() {
        User user = buildUser();
        UpdateUserProfileRequest request = new UpdateUserProfileRequest(
                null,
                null,
                "currentPass",
                "NewPassword1",
                "NewPassword1"
        );

        when(authService.getAuthenticatedUser(authentication)).thenReturn(user);
        when(passwordEncoder.matches("currentPass", user.getPassword())).thenReturn(true);
        when(passwordEncoder.encode("NewPassword1")).thenReturn("encoded-new");
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.updateProfile(authentication, request);

        assertThat(updated.getPassword()).isEqualTo("encoded-new");
    }

    @Test
    void updateUserRoleShouldApplyNewRole() {
        User admin = buildUser();
        admin.setId("admin-1");
        admin.setRole(Role.ADMIN);
        User target = buildUser();
        target.setId("user-2");
        target.setRole(Role.RECRUITER);

        when(authService.getAuthenticatedUser(authentication)).thenReturn(admin);
        when(userRepository.findById("user-2")).thenReturn(Optional.of(target));
        when(userRepository.save(target)).thenReturn(target);

        User updated = userService.updateUserRole(authentication, "user-2", Role.HR_MANAGER);

        assertThat(updated.getRole()).isEqualTo(Role.HR_MANAGER);
    }

    @Test
    void updateUserRoleShouldRejectSelfChange() {
        User admin = buildUser();
        admin.setId("admin-1");
        admin.setRole(Role.ADMIN);

        when(authService.getAuthenticatedUser(authentication)).thenReturn(admin);

        assertThatThrownBy(() -> userService.updateUserRole(authentication, "admin-1", Role.RECRUITER))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("own role");
    }

    @Test
    void updateUserRoleShouldRejectMissingUser() {
        User admin = buildUser();
        admin.setId("admin-1");
        admin.setRole(Role.ADMIN);

        when(authService.getAuthenticatedUser(authentication)).thenReturn(admin);
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserRole(authentication, "missing", Role.RECRUITER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listUsersForAdminShouldDelegateToRepository() {
        User u1 = buildUser();
        when(userRepository.findByDeletedFalseOrderByUsernameAsc()).thenReturn(List.of(u1));

        List<User> users = userService.listUsersForAdmin();

        assertThat(users).containsExactly(u1);
    }

    private User buildUser() {
        User user = new User();
        user.setId("user-1");
        user.setUsername("recruiter");
        user.setEmail("recruiter@rts.com");
        user.setPassword("encoded-current");
        user.setRole(Role.RECRUITER);
        return user;
    }
}
