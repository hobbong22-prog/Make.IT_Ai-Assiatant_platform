package com.Human.Ai.D.makit.controller;

import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.domain.UserRole;
import com.Human.Ai.D.makit.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for managing role-based access control
 */
@RestController
@RequestMapping("/api/admin/roles")
public class RoleBasedAccessController {

    @Autowired
    private AuthService authService;

    /**
     * Get all available user roles
     */
    @GetMapping("/available")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserRole>> getAvailableRoles() {
        return ResponseEntity.ok(Arrays.asList(UserRole.values()));
    }

    /**
     * Update user role (Admin only)
     */
    @PutMapping("/user/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRole(
            @PathVariable Long userId,
            @RequestBody Map<String, String> request) {
        try {
            String roleCode = request.get("role");
            if (roleCode == null || roleCode.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Role is required"));
            }

            UserRole newRole = UserRole.fromCode(roleCode);
            User updatedUser = authService.updateUserRole(userId, newRole);
            
            return ResponseEntity.ok(Map.of(
                "message", "User role updated successfully",
                "userId", updatedUser.getId(),
                "newRole", updatedUser.getUserRole().getCode()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid role: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update user role: " + e.getMessage()));
        }
    }

    /**
     * Get user permissions based on role
     */
    @GetMapping("/user/{userId}/permissions")
    @PreAuthorize("hasRole('ADMIN') or @authService.isCurrentUser(#userId)")
    public ResponseEntity<?> getUserPermissions(@PathVariable Long userId) {
        try {
            User user = authService.findById(userId);
            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            UserRole role = user.getUserRole();
            Map<String, Boolean> permissions = Map.of(
                "canManageContent", role.canManageContent(),
                "canManageCampaigns", role.canManageCampaigns(),
                "canAccessAnalytics", role.canAccessAnalytics(),
                "canManageUsers", role.canManageUsers(),
                "isAdmin", role.isAdmin()
            );

            return ResponseEntity.ok(Map.of(
                "userId", userId,
                "role", role.getCode(),
                "roleName", role.getDisplayName(),
                "permissions", permissions
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get user permissions: " + e.getMessage()));
        }
    }

    /**
     * Activate/Deactivate user (Admin only)
     */
    @PutMapping("/user/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody Map<String, Boolean> request) {
        try {
            Boolean isActive = request.get("isActive");
            if (isActive == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "isActive status is required"));
            }

            User updatedUser = authService.updateUserStatus(userId, isActive);
            
            return ResponseEntity.ok(Map.of(
                "message", "User status updated successfully",
                "userId", updatedUser.getId(),
                "isActive", updatedUser.isActive()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update user status: " + e.getMessage()));
        }
    }

    /**
     * Get all users with their roles (Admin only)
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsersWithRoles() {
        try {
            List<User> users = authService.getAllUsers();
            List<Map<String, Object>> userSummaries = users.stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("email", user.getEmail());
                    userMap.put("role", user.getUserRole().getCode());
                    userMap.put("roleName", user.getUserRole().getDisplayName());
                    userMap.put("isActive", user.isActive());
                    userMap.put("lastLoginAt", user.getLastLoginAt());
                    userMap.put("companyName", user.getCompanyName() != null ? user.getCompanyName() : "");
                    return userMap;
                })
                .collect(java.util.stream.Collectors.toList());

            return ResponseEntity.ok(userSummaries);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get users: " + e.getMessage()));
        }
    }
}