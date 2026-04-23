package com.rts.modules.auth.api;

import com.rts.modules.auth.api.dto.LoginRequest;
import com.rts.modules.auth.api.dto.LoginResponse;
import com.rts.modules.auth.application.AuthService;
import com.rts.shared.response.ApiResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Login", description = "Validate credentials and return user identity and role. "
            + "This endpoint is used by the frontend login flow and validates credentials from request body.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse body = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", body));
    }
}
