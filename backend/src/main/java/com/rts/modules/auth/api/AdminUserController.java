package com.rts.modules.auth.api;

import com.rts.modules.auth.api.dto.UpdateUserRoleRequest;
import com.rts.modules.auth.api.dto.UserProfileResponse;
import com.rts.modules.auth.application.UserService;
import com.rts.modules.auth.domain.User;
import com.rts.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin — Users")
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "List users", description = "Returns all active users. Restricted to ADMIN.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> listUsers() {
        List<UserProfileResponse> rows = userService.listUsersForAdmin().stream()
                .map(UserProfileResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", rows));
    }

    @Operation(summary = "Update user role", description = "Sets a user's role. ADMIN cannot change their own role.")
    @PutMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserRole(
            Authentication authentication,
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        User updated = userService.updateUserRole(authentication, userId, request.role());
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully", UserProfileResponse.from(updated)));
    }
}
