package com.Human.Ai.D.makit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.domain.UserRole;
import com.Human.Ai.D.makit.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for RoleBasedAccessController
 */
@WebMvcTest(RoleBasedAccessController.class)
public class RoleBasedAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private User adminUser;

    @BeforeEach
    public void setUp() {
        testUser = new User("testuser", "test@example.com", "password", "Test Company");
        testUser.setId(1L);
        testUser.setUserRole(UserRole.CONTENT_CREATOR);
        testUser.setActive(true);
        testUser.setLastLoginAt(LocalDateTime.now());

        adminUser = new User("admin", "admin@example.com", "password", "Test Company");
        adminUser.setId(2L);
        adminUser.setUserRole(UserRole.ADMIN);
        adminUser.setActive(true);
        adminUser.setLastLoginAt(LocalDateTime.now());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetAvailableRoles() throws Exception {
        mockMvc.perform(get("/api/admin/roles/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0]").value("ADMIN"))
                .andExpect(jsonPath("$[1]").value("MARKETING_MANAGER"))
                .andExpect(jsonPath("$[2]").value("CONTENT_CREATOR"))
                .andExpect(jsonPath("$[3]").value("ANALYST"))
                .andExpect(jsonPath("$[4]").value("VIEWER"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testGetAvailableRolesUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/roles/available"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUpdateUserRole() throws Exception {
        when(authService.updateUserRole(eq(1L), eq(UserRole.MARKETING_MANAGER)))
                .thenReturn(testUser);

        Map<String, String> request = Map.of("role", "MARKETING_MANAGER");

        mockMvc.perform(put("/api/admin/roles/user/1/role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User role updated successfully"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.newRole").value("CONTENT_CREATOR"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUpdateUserRoleInvalidRole() throws Exception {
        Map<String, String> request = Map.of("role", "INVALID_ROLE");

        mockMvc.perform(put("/api/admin/roles/user/1/role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Invalid role")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUpdateUserRoleMissingRole() throws Exception {
        Map<String, String> request = Map.of();

        mockMvc.perform(put("/api/admin/roles/user/1/role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Role is required"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetUserPermissions() throws Exception {
        when(authService.findById(1L)).thenReturn(testUser);

        mockMvc.perform(get("/api/admin/roles/user/1/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.role").value("CONTENT_CREATOR"))
                .andExpect(jsonPath("$.roleName").value("Content Creator"))
                .andExpect(jsonPath("$.permissions.canManageContent").value(true))
                .andExpect(jsonPath("$.permissions.canManageCampaigns").value(false))
                .andExpect(jsonPath("$.permissions.canAccessAnalytics").value(false))
                .andExpect(jsonPath("$.permissions.canManageUsers").value(false))
                .andExpect(jsonPath("$.permissions.isAdmin").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetUserPermissionsUserNotFound() throws Exception {
        when(authService.findById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/admin/roles/user/999/permissions"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUpdateUserStatus() throws Exception {
        when(authService.updateUserStatus(eq(1L), eq(false)))
                .thenReturn(testUser);

        Map<String, Boolean> request = Map.of("isActive", false);

        mockMvc.perform(put("/api/admin/roles/user/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User status updated successfully"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testUpdateUserStatusMissingStatus() throws Exception {
        Map<String, String> request = Map.of();

        mockMvc.perform(put("/api/admin/roles/user/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("isActive status is required"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testGetAllUsersWithRoles() throws Exception {
        List<User> users = Arrays.asList(testUser, adminUser);
        when(authService.getAllUsers()).thenReturn(users);

        mockMvc.perform(get("/api/admin/roles/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("testuser"))
                .andExpect(jsonPath("$[0].role").value("CONTENT_CREATOR"))
                .andExpect(jsonPath("$[0].roleName").value("Content Creator"))
                .andExpect(jsonPath("$[0].isActive").value(true))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].username").value("admin"))
                .andExpect(jsonPath("$[1].role").value("ADMIN"))
                .andExpect(jsonPath("$[1].roleName").value("Administrator"))
                .andExpect(jsonPath("$[1].isActive").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void testAdminOnlyEndpointsUnauthorized() throws Exception {
        // Test that non-admin users cannot access admin endpoints
        mockMvc.perform(put("/api/admin/roles/user/1/role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/roles/user/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":false}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/roles/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser", roles = "CONTENT_CREATOR")
    public void testUserCanAccessOwnPermissions() throws Exception {
        when(authService.findById(1L)).thenReturn(testUser);
        when(authService.isCurrentUser(1L)).thenReturn(true);

        mockMvc.perform(get("/api/admin/roles/user/1/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.role").value("CONTENT_CREATOR"));
    }

    @Test
    @WithMockUser(username = "otheruser", roles = "CONTENT_CREATOR")
    public void testUserCannotAccessOtherUserPermissions() throws Exception {
        when(authService.isCurrentUser(1L)).thenReturn(false);

        mockMvc.perform(get("/api/admin/roles/user/1/permissions"))
                .andExpect(status().isForbidden());
    }
}