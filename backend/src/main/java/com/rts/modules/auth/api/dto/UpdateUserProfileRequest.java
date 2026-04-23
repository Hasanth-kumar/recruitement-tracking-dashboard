package com.rts.modules.auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
        String username,

        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,

        String currentPassword,
        String newPassword,
        String confirmNewPassword
) {
}
