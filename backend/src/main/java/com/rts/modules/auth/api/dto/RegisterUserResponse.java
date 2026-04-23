package com.rts.modules.auth.api.dto;

import com.rts.modules.auth.domain.User;

public record RegisterUserResponse(
        String id,
        String username,
        String email,
        String role
) {
    public static RegisterUserResponse from(User user) {
        return new RegisterUserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
