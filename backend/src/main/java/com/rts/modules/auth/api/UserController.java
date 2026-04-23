package com.rts.modules.auth.api;

import com.rts.modules.auth.api.dto.UpdateUserProfileRequest;
import com.rts.modules.auth.api.dto.UserProfileResponse;
import com.rts.modules.auth.application.UserService;
import com.rts.modules.auth.domain.User;
import com.rts.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users")
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get profile", description = "Returns authenticated user's profile information.")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(Authentication authentication) {
        User user = userService.getProfile(authentication);
        return ResponseEntity.ok(ApiResponse.success("Profile fetched successfully", UserProfileResponse.from(user)));
    }

    @Operation(summary = "Update profile", description = "Updates authenticated user's profile, including optional password change.")
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        User user = userService.updateProfile(authentication, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", UserProfileResponse.from(user)));
    }
}
