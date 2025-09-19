package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.domain.UserRole;
import com.Human.Ai.D.makit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService role-based functionality
 */
@ExtendWith(MockitoExtension.class)
public class AuthServiceRoleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private User adminUser;
    private User managerUser;

    @BeforeEach
    public void setUp() {
        testUser = new User("testuser", "test@example.com", "password", "Test Company");
        testUser.setId(1L);
        testUser.setUserRole(UserRole.CONTENT_CREATOR);
        testUser.setActive(true);

        adminUser = new User("admin", "admin@example.com", "password", "Test Company");
        adminUser.setId(2L);
        adminUser.setUserRole(UserRole.ADMIN);
        adminUser.setActive(true);

        managerUser = new User("manager", "manager@example.com", "password", "Test Company");
        managerUser.setId(3L);
        managerUser.setUserRole(UserRole.MARKETING_MANAGER);
        managerUser.setActive(true);
    }

    @Test
    public void testUpdateUserRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User updatedUser = authService.updateUserRole(1L, UserRole.MARKETING_MANAGER);

        assertEquals(UserRole.MARKETING_MANAGER, updatedUser.getUserRole());
        verify(userRepository).save(testUser);
    }

    @Test
    public void testUpdateUserRoleUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authService.updateUserRole(999L, UserRole.ADMIN);
        });
    }

    @Test
    public void testUpdateUserStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User updatedUser = authService.updateUserStatus(1L, false);

        assertFalse(updatedUser.isActive());
        verify(userRepository).save(testUser);
    }

    @Test
    public void testUpdateUserStatusUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authService.updateUserStatus(999L, false);
        });
    }

    @Test
    public void testFindById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        User foundUser = authService.findById(1L);

        assertNotNull(foundUser);
        assertEquals(1L, foundUser.getId());
        assertNull(foundUser.getPassword()); // Password should be removed
    }

    @Test
    public void testFindByIdNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        User foundUser = authService.findById(999L);

        assertNull(foundUser);
    }

    @Test
    public void testGetAllUsers() {
        List<User> users = Arrays.asList(testUser, adminUser, managerUser);
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = authService.getAllUsers();

        assertEquals(3, result.size());
        // Verify passwords are removed
        result.forEach(user -> {
            assertNull(user.getPassword());
            assertNull(user.getCampaigns());
            assertNull(user.getContents());
        });
    }

    @Test
    public void testUpdateLastLogin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.updateLastLogin(1L);

        assertNotNull(testUser.getLastLoginAt());
        verify(userRepository).save(testUser);
    }

    @Test
    public void testUpdateLastLoginUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Should not throw exception, just do nothing
        assertDoesNotThrow(() -> {
            authService.updateLastLogin(999L);
        });
    }

    @Test
    public void testHasRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        assertTrue(authService.hasRole(1L, UserRole.CONTENT_CREATOR));
        assertFalse(authService.hasRole(1L, UserRole.ADMIN));
    }

    @Test
    public void testCanManageContent() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));

        assertTrue(authService.canManageContent(1L)); // Content creator can manage content
        assertTrue(authService.canManageContent(2L)); // Admin can manage content
    }

    @Test
    public void testCanManageCampaigns() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(managerUser));

        assertFalse(authService.canManageCampaigns(1L)); // Content creator cannot manage campaigns
        assertTrue(authService.canManageCampaigns(2L)); // Admin can manage campaigns
        assertTrue(authService.canManageCampaigns(3L)); // Marketing manager can manage campaigns
    }

    @Test
    public void testCanAccessAnalytics() {
        User analystUser = new User("analyst", "analyst@example.com", "password", "Test Company");
        analystUser.setId(4L);
        analystUser.setUserRole(UserRole.ANALYST);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(3L)).thenReturn(Optional.of(managerUser));
        when(userRepository.findById(4L)).thenReturn(Optional.of(analystUser));

        assertFalse(authService.canAccessAnalytics(1L)); // Content creator cannot access analytics
        assertTrue(authService.canAccessAnalytics(2L)); // Admin can access analytics
        assertTrue(authService.canAccessAnalytics(3L)); // Marketing manager can access analytics
        assertTrue(authService.canAccessAnalytics(4L)); // Analyst can access analytics
    }

    @Test
    public void testCanManageUsers() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));

        assertFalse(authService.canManageUsers(1L)); // Content creator cannot manage users
        assertTrue(authService.canManageUsers(2L)); // Admin can manage users
    }

    @Test
    public void testPermissionMethodsWithNullUser() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertFalse(authService.canManageContent(999L));
        assertFalse(authService.canManageCampaigns(999L));
        assertFalse(authService.canAccessAnalytics(999L));
        assertFalse(authService.canManageUsers(999L));
        assertFalse(authService.hasRole(999L, UserRole.ADMIN));
    }
}