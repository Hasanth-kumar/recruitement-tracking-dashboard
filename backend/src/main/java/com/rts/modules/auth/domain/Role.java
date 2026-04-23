package com.rts.modules.auth.domain;

import org.springframework.security.core.GrantedAuthority;

public enum Role implements GrantedAuthority {
    ADMIN,
    HR_MANAGER,
    RECRUITER,
    INTERVIEWER;

    @Override
    public String getAuthority() {
        return "ROLE_" + name();
    }
}
