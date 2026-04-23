package com.rts.modules.auth.api.dto;

import com.rts.modules.auth.domain.Role;

public record LoginResponse(
        UserInfo user
) {

    public static LoginResponse of(String userId, String username, String email, Role role) {
        return new LoginResponse(new UserInfo(userId, username, email, role.name()));
    }

    public record UserInfo(String id, String username, String email, String role) {
    }
}
