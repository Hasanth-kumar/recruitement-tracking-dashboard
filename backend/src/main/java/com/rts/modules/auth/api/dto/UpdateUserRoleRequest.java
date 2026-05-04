package com.rts.modules.auth.api.dto;

import com.rts.modules.auth.domain.Role;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {
}
