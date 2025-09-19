package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.domain.UserRole;
import com.Human.Ai.D.makit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for role-based access control functionality
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class RoleBasedAccessControlIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User adminUser;
    private User managerUser;
    private User analystUser;
    private User viewerUser;

    @BeforeEach
    public void setUp() {
        // Clear any existing users
        userRepository.deleteAll();

        // Create test users with different roles
        testUser = createUser("content_creator", "creator@test.com", UserRole.CONTENT_CREATOR);
        adminUser = createUser("admin", "admin@test.com", UserRole.ADMIN);
        managerUser = createUser("manager", "manager@test.com", UserRole.MARKETING_MANAGER);
        analystUser = createUser("analyst", "analyst@test.com", UserRole.ANALYST);
        viewerUser = createUser("viewer", "viewer@test.com", UserRole.VIEWER);
    }

    private User createUser(String username, String email, UserRole role) {
        User user = new User(username, email, passwordEncoder.encode("password"), "Test Company", role);
        user.setActive(true);
        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    @Test
    public void testUserCreationWithRoles() {
        assertEquals(5, userRepository.count());
        
        User foundAdmin = userRepository.findByEmail("admin@test.com").orElse(null);
        assertNotNull(foundAdmin);
        assertEquals(UserRole.ADMIN, foundAdmin.getUserRole());
        assertTrue(foundAdmin.isActive());
    }

    @Test
    public void testUpdateUserRole() {
        // Update content creator to marketing manager
        User updatedUser = authService.updateUserRole(testUser.getId(), UserRole.MARKETING_MANAGER);
        
        assertEquals(UserRole.MARKETING_MANAGER, updatedUser.getUserRole());
        
        // Verify in database
        User fromDb = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(fromDb);
        assertEquals(UserRole.MARKETING_MANAGER, fromDb.getUserRole());
    }

    @Test
    public void testUpdateUserStatus() {
        // Deactivate user
        User updatedUser = authService.updateUserStatus(testUser.getId(), false);
        
        assertFalse(updatedUser.isActive());
        
        // Verify in database
        User fromDb = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(fromDb);
        assertFalse(fromDb.isActive());
    }

    @Test
    public void testGetAllUsers() {
        List<User> users = authService.getAllUsers();
        
        assertEquals(5, users.size());
        
        // Verify passwords are removed
        users.forEach(user -> {
            assertNull(user.getPassword());
            assertNull(user.getCampaigns());
            assertNull(user.getContents());
        });
        
        // Verify all roles are present
        assertTrue(users.stream().anyMatch(u -> u.getUserRole() == UserRole.ADMIN));
        assertTrue(users.stream().anyMatch(u -> u.getUserRole() == UserRole.MARKETING_MANAGER));
        assertTrue(users.stream().anyMatch(u -> u.getUserRole() == UserRole.CONTENT_CREATOR));
        assertTrue(users.stream().anyMatch(u -> u.getUserRole() == UserRole.ANALYST));
        assertTrue(users.stream().anyMatch(u -> u.getUserRole() == UserRole.VIEWER));
    }

    @Test
    public void testRolePermissions() {
        // Test admin permissions
        assertTrue(authService.canManageContent(adminUser.getId()));
        assertTrue(authService.canManageCampaigns(adminUser.getId()));
        assertTrue(authService.canAccessAnalytics(adminUser.getId()));
        assertTrue(authService.canManageUsers(adminUser.getId()));
        assertTrue(authService.hasRole(adminUser.getId(), UserRole.ADMIN));

        // Test marketing manager permissions
        assertTrue(authService.canManageContent(managerUser.getId()));
        assertTrue(authService.canManageCampaigns(managerUser.getId()));
        assertTrue(authService.canAccessAnalytics(managerUser.getId()));
        assertFalse(authService.canManageUsers(managerUser.getId()));
        assertTrue(authService.hasRole(managerUser.getId(), UserRole.MARKETING_MANAGER));

        // Test content creator permissions
        assertTrue(authService.canManageContent(testUser.getId()));
        assertFalse(authService.canManageCampaigns(testUser.getId()));
        assertFalse(authService.canAccessAnalytics(testUser.getId()));
        assertFalse(authService.canManageUsers(testUser.getId()));
        assertTrue(authService.hasRole(testUser.getId(), UserRole.CONTENT_CREATOR));

        // Test analyst permissions
        assertFalse(authService.canManageContent(analystUser.getId()));
        assertFalse(authService.canManageCampaigns(analystUser.getId()));
        assertTrue(authService.canAccessAnalytics(analystUser.getId()));
        assertFalse(authService.canManageUsers(analystUser.getId()));
        assertTrue(authService.hasRole(analystUser.getId(), UserRole.ANALYST));

        // Test viewer permissions
        assertFalse(authService.canManageContent(viewerUser.getId()));
        assertFalse(authService.canManageCampaigns(viewerUser.getId()));
        assertFalse(authService.canAccessAnalytics(viewerUser.getId()));
        assertFalse(authService.canManageUsers(viewerUser.getId()));
        assertTrue(authService.hasRole(viewerUser.getId(), UserRole.VIEWER));
    }

    @Test
    public void testUpdateLastLogin() {
        LocalDateTime beforeUpdate = testUser.getLastLoginAt();
        
        // Wait a bit to ensure time difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        authService.updateLastLogin(testUser.getId());
        
        User updatedUser = userRepository.findById(testUser.getId()).orElse(null);
        assertNotNull(updatedUser);
        assertNotNull(updatedUser.getLastLoginAt());
        assertTrue(updatedUser.getLastLoginAt().isAfter(beforeUpdate));
    }

    @Test
    public void testFindById() {
        User foundUser = authService.findById(testUser.getId());
        
        assertNotNull(foundUser);
        assertEquals(testUser.getId(), foundUser.getId());
        assertEquals(testUser.getUsername(), foundUser.getUsername());
        assertEquals(testUser.getEmail(), foundUser.getEmail());
        assertEquals(testUser.getUserRole(), foundUser.getUserRole());
        assertNull(foundUser.getPassword()); // Password should be removed
    }

    @Test
    public void testRoleBasedUserCreation() {
        // Test that new users get default VIEWER role
        User newUser = new User("newuser", "new@test.com", "password", "New Company");
        User savedUser = userRepository.save(newUser);
        
        assertEquals(UserRole.VIEWER, savedUser.getUserRole());
        assertTrue(savedUser.isActive());
    }

    @Test
    public void testUserPreferences() {
        // Test that user preferences are properly handled
        testUser.getPreferences().put("theme", "dark");
        testUser.getPreferences().put("language", "en");
        User savedUser = userRepository.save(testUser);
        
        User foundUser = userRepository.findById(savedUser.getId()).orElse(null);
        assertNotNull(foundUser);
        assertEquals("dark", foundUser.getPreferences().get("theme"));
        assertEquals("en", foundUser.getPreferences().get("language"));
    }

    @Test
    public void testInvalidOperations() {
        // Test updating role for non-existent user
        assertThrows(RuntimeException.class, () -> {
            authService.updateUserRole(999L, UserRole.ADMIN);
        });

        // Test updating status for non-existent user
        assertThrows(RuntimeException.class, () -> {
            authService.updateUserStatus(999L, false);
        });

        // Test finding non-existent user
        User notFound = authService.findById(999L);
        assertNull(notFound);
    }
}