package com.rts.modules.auth.api;

import com.rts.modules.auth.api.dto.ChangeEmailRequest;
import com.rts.modules.auth.application.AuthService;
import com.rts.modules.auth.application.EmailVerificationService;
import com.rts.modules.auth.domain.User;
import com.rts.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Profile")
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public ProfileController(AuthService authService, EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

    @Operation(summary = "Request email change",
            description = "Initiates an email change. A verification link is sent to the new email address.")
    @PostMapping("/change-email")
    public ResponseEntity<ApiResponse<Void>> changeEmail(
            Authentication authentication,
            @Valid @RequestBody ChangeEmailRequest request
    ) {
        User user = authService.getAuthenticatedUser(authentication);
        emailVerificationService.initiateEmailChange(user, request.newEmail());
        return ResponseEntity.ok(ApiResponse.success(
                "Verification email sent to " + request.newEmail() + ". Please check your inbox.", null));
    }

    @Operation(summary = "Resend email verification",
            description = "Resends the verification email for a pending email change.")
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(Authentication authentication) {
        User user = authService.getAuthenticatedUser(authentication);
        emailVerificationService.resendVerification(user);
        return ResponseEntity.ok(ApiResponse.success("Verification email resent successfully.", null));
    }
}
