package com.rts.modules.auth.api.dto;

import com.rts.modules.auth.domain.Role;
import com.rts.modules.auth.domain.User;

public record UserProfileResponse(
        String id,
        String username,
        String email,
        Role role
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole()
        );
    }
}
