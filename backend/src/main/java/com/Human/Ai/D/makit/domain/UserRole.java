package com.Human.Ai.D.makit.domain;

/**
 * Enumeration defining user roles in the MarKIT platform
 * Each role has specific permissions and access levels
 */
public enum UserRole {
    /**
     * Administrator with full system access
     */
    ADMIN("ADMIN", "Administrator", "Full system access and user management"),
    
    /**
     * Marketing manager with campaign and content management access
     */
    MARKETING_MANAGER("MARKETING_MANAGER", "Marketing Manager", "Campaign and content management access"),
    
    /**
     * Content creator with content creation and editing access
     */
    CONTENT_CREATOR("CONTENT_CREATOR", "Content Creator", "Content creation and editing access"),
    
    /**
     * Analyst with read access to analytics and reports
     */
    ANALYST("ANALYST", "Analyst", "Analytics and reporting access"),
    
    /**
     * Viewer with read-only access
     */
    VIEWER("VIEWER", "Viewer", "Read-only access to platform");

    private final String code;
    private final String displayName;
    private final String description;

    UserRole(String code, String displayName, String description) {
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get UserRole from string code
     */
    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid role code: " + code);
    }

    /**
     * Check if this role has administrative privileges
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * Check if this role can manage content
     */
    public boolean canManageContent() {
        return this == ADMIN || this == MARKETING_MANAGER || this == CONTENT_CREATOR;
    }

    /**
     * Check if this role can manage campaigns
     */
    public boolean canManageCampaigns() {
        return this == ADMIN || this == MARKETING_MANAGER;
    }

    /**
     * Check if this role can access analytics
     */
    public boolean canAccessAnalytics() {
        return this == ADMIN || this == MARKETING_MANAGER || this == ANALYST;
    }

    /**
     * Check if this role can manage users
     */
    public boolean canManageUsers() {
        return this == ADMIN;
    }
}