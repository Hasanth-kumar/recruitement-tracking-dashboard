package com.rts.modules.auth.api;

import com.rts.modules.auth.api.dto.LoginRequest;
import com.rts.modules.auth.api.dto.LoginResponse;
import com.rts.modules.auth.api.dto.UserProfileResponse;
import com.rts.modules.auth.application.AuthService;
import com.rts.modules.auth.application.EmailVerificationService;
import com.rts.modules.auth.domain.User;
import com.rts.shared.response.ApiResponse;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(AuthService authService, EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

    @Operation(summary = "Login", description = "Validate credentials and return JWT access token with user info.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse body = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", body));
    }

    @Operation(summary = "Get current user", description = "Returns the authenticated user's profile from the JWT token.")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> me(Authentication authentication) {
        User user = authService.getAuthenticatedUser(authentication);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", UserProfileResponse.from(user)));
    }

    @Operation(summary = "Verify email", description = "Confirms a new email address using the verification token sent via email.")
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestParam String token) {
        emailVerificationService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully.", null));
    }
}
