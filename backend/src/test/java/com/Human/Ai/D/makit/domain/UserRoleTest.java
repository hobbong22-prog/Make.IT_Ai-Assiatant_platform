package com.Human.Ai.D.makit.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UserRole enum
 */
public class UserRoleTest {

    @Test
    public void testUserRoleValues() {
        assertEquals(5, UserRole.values().length);
        assertEquals("ADMIN", UserRole.ADMIN.getCode());
        assertEquals("MARKETING_MANAGER", UserRole.MARKETING_MANAGER.getCode());
        assertEquals("CONTENT_CREATOR", UserRole.CONTENT_CREATOR.getCode());
        assertEquals("ANALYST", UserRole.ANALYST.getCode());
        assertEquals("VIEWER", UserRole.VIEWER.getCode());
    }

    @Test
    public void testFromCode() {
        assertEquals(UserRole.ADMIN, UserRole.fromCode("ADMIN"));
        assertEquals(UserRole.MARKETING_MANAGER, UserRole.fromCode("MARKETING_MANAGER"));
        assertEquals(UserRole.CONTENT_CREATOR, UserRole.fromCode("CONTENT_CREATOR"));
        assertEquals(UserRole.ANALYST, UserRole.fromCode("ANALYST"));
        assertEquals(UserRole.VIEWER, UserRole.fromCode("VIEWER"));
    }

    @Test
    public void testFromCodeInvalid() {
        assertThrows(IllegalArgumentException.class, () -> {
            UserRole.fromCode("INVALID_ROLE");
        });
    }

    @Test
    public void testIsAdmin() {
        assertTrue(UserRole.ADMIN.isAdmin());
        assertFalse(UserRole.MARKETING_MANAGER.isAdmin());
        assertFalse(UserRole.CONTENT_CREATOR.isAdmin());
        assertFalse(UserRole.ANALYST.isAdmin());
        assertFalse(UserRole.VIEWER.isAdmin());
    }

    @Test
    public void testCanManageContent() {
        assertTrue(UserRole.ADMIN.canManageContent());
        assertTrue(UserRole.MARKETING_MANAGER.canManageContent());
        assertTrue(UserRole.CONTENT_CREATOR.canManageContent());
        assertFalse(UserRole.ANALYST.canManageContent());
        assertFalse(UserRole.VIEWER.canManageContent());
    }

    @Test
    public void testCanManageCampaigns() {
        assertTrue(UserRole.ADMIN.canManageCampaigns());
        assertTrue(UserRole.MARKETING_MANAGER.canManageCampaigns());
        assertFalse(UserRole.CONTENT_CREATOR.canManageCampaigns());
        assertFalse(UserRole.ANALYST.canManageCampaigns());
        assertFalse(UserRole.VIEWER.canManageCampaigns());
    }

    @Test
    public void testCanAccessAnalytics() {
        assertTrue(UserRole.ADMIN.canAccessAnalytics());
        assertTrue(UserRole.MARKETING_MANAGER.canAccessAnalytics());
        assertFalse(UserRole.CONTENT_CREATOR.canAccessAnalytics());
        assertTrue(UserRole.ANALYST.canAccessAnalytics());
        assertFalse(UserRole.VIEWER.canAccessAnalytics());
    }

    @Test
    public void testCanManageUsers() {
        assertTrue(UserRole.ADMIN.canManageUsers());
        assertFalse(UserRole.MARKETING_MANAGER.canManageUsers());
        assertFalse(UserRole.CONTENT_CREATOR.canManageUsers());
        assertFalse(UserRole.ANALYST.canManageUsers());
        assertFalse(UserRole.VIEWER.canManageUsers());
    }

    @Test
    public void testDisplayNames() {
        assertEquals("Administrator", UserRole.ADMIN.getDisplayName());
        assertEquals("Marketing Manager", UserRole.MARKETING_MANAGER.getDisplayName());
        assertEquals("Content Creator", UserRole.CONTENT_CREATOR.getDisplayName());
        assertEquals("Analyst", UserRole.ANALYST.getDisplayName());
        assertEquals("Viewer", UserRole.VIEWER.getDisplayName());
    }

    @Test
    public void testDescriptions() {
        assertNotNull(UserRole.ADMIN.getDescription());
        assertNotNull(UserRole.MARKETING_MANAGER.getDescription());
        assertNotNull(UserRole.CONTENT_CREATOR.getDescription());
        assertNotNull(UserRole.ANALYST.getDescription());
        assertNotNull(UserRole.VIEWER.getDescription());
        
        assertTrue(UserRole.ADMIN.getDescription().contains("Full system access"));
        assertTrue(UserRole.MARKETING_MANAGER.getDescription().contains("Campaign"));
        assertTrue(UserRole.CONTENT_CREATOR.getDescription().contains("Content"));
        assertTrue(UserRole.ANALYST.getDescription().contains("Analytics"));
        assertTrue(UserRole.VIEWER.getDescription().contains("Read-only"));
    }
}