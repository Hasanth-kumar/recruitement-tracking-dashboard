package com.rts.modules.auth.application;

import com.rts.modules.auth.api.dto.UpdateUserProfileRequest;
import com.rts.modules.auth.domain.User;
import com.rts.modules.auth.persistence.UserRepository;
import com.rts.shared.exception.ConflictException;
import com.rts.shared.exception.ValidationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AuthService authService, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER', 'INTERVIEWER')")
    @Transactional(readOnly = true)
    public User getProfile(Authentication authentication) {
        return authService.getAuthenticatedUser(authentication);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER', 'RECRUITER', 'INTERVIEWER')")
    @Transactional
    public User updateProfile(Authentication authentication, UpdateUserProfileRequest request) {
        User user = authService.getAuthenticatedUser(authentication);

        if (StringUtils.hasText(request.username())) {
            String username = request.username().trim();
            userRepository.findByUsername(username)
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> {
                        throw new ConflictException("Username already in use");
                    });
            user.setUsername(username);
        }

        if (StringUtils.hasText(request.email())) {
            String email = request.email().trim().toLowerCase();
            userRepository.findByEmail(email)
                    .filter(existing -> !existing.getId().equals(user.getId()))
                    .ifPresent(existing -> {
                        throw new ConflictException("Email already in use");
                    });
            user.setEmail(email);
        }

        applyPasswordChangeIfPresent(user, request);
        return userRepository.save(user);
    }

    private void applyPasswordChangeIfPresent(User user, UpdateUserProfileRequest request) {
        boolean hasAnyPasswordField = StringUtils.hasText(request.currentPassword())
                || StringUtils.hasText(request.newPassword())
                || StringUtils.hasText(request.confirmNewPassword());

        if (!hasAnyPasswordField) {
            return;
        }

        if (!StringUtils.hasText(request.currentPassword())
                || !StringUtils.hasText(request.newPassword())
                || !StringUtils.hasText(request.confirmNewPassword())) {
            throw new ValidationException("Current password, new password and confirm password are required");
        }

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ValidationException("Current password is incorrect");
        }
        if (request.newPassword().length() < 8) {
            throw new ValidationException("New password must be at least 8 characters");
        }
        if (request.newPassword().equals(request.currentPassword())) {
            throw new ValidationException("New password must be different from current password");
        }
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new ValidationException("New password and confirm password do not match");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }
}
