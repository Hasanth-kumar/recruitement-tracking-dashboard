package com.rts.modules.candidate.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCandidateRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name cannot exceed 150 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email cannot exceed 255 characters")
        String email,

        @NotBlank(message = "Phone is required")
        @Pattern(regexp = "^[0-9+\\-()\\s]{8,20}$", message = "Phone format is invalid")
        String phone,

        @NotBlank(message = "Position is required")
        @Size(max = 100, message = "Position cannot exceed 100 characters")
        String position,

        @Size(max = 200, message = "Experience cannot exceed 200 characters")
        String experience,

        @Size(max = 500, message = "Notes cannot exceed 500 characters")
        String notes
) {
}
