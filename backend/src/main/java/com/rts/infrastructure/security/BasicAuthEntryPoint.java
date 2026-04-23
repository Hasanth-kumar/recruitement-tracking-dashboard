package com.rts.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rts.shared.response.ApiResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class BasicAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public BasicAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String message = authException instanceof BadCredentialsException
                ? "Invalid username or password"
                : "Authentication required";
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failure(message)));
    }
}
