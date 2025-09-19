package com.Human.Ai.D.makit.integration;

import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.domain.UserRole;
import com.Human.Ai.D.makit.repository.UserRepository;
import com.Human.Ai.D.makit.service.AuthService;
import com.Human.Ai.D.makit.service.CognitoAuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic integration test for Cognito authentication functionality
 * This test verifies that the Cognito integration components are properly wired
 * and the basic functionality works without mocking AWS services.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CognitoIntegrationBasicTest {
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private CognitoAuthenticationService cognitoAuthService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void testCognitoServiceIsWired() {
        // Verify that the Cognito service is properly injected
        assertNotNull(cognitoAuthService);
        assertNotNull(authService);
    }
    
    @Test
    void testUserRepositoryHasCognitoUserIdSupport() {
        // Test that the UserRepository supports finding by Cognito User ID
        User testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setCompanyName("Test Company");
        testUser.setCognitoUserId("cognito-test-123");
        testUser.setUserRole(UserRole.VIEWER);
        
        User savedUser = userRepository.save(testUser);
        assertNotNull(savedUser.getId());
        
        // Test finding by Cognito User ID
        var foundUser = userRepository.findByCognitoUserId("cognito-test-123");
        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());
        assertEquals("cognito-test-123", foundUser.get().getCognitoUserId());
    }
    
    @Test
    void testUserEntityHasCognitoFields() {
        // Verify that User entity has all required Cognito fields
        User user = new User();
        
        // Test Cognito User ID field
        user.setCognitoUserId("cognito-123");
        assertEquals("cognito-123", user.getCognitoUserId());
        
        // Test Company ID field
        user.setCompanyId("company-456");
        assertEquals("company-456", user.getCompanyId());
        
        // Test preferences field
        user.getPreferences().put("theme", "dark");
        assertEquals("dark", user.getPreferences().get("theme"));
        
        // Test active status field
        user.setActive(true);
        assertTrue(user.isActive());
        
        // Test last login field
        assertNull(user.getLastLoginAt()); // Should be null initially
    }
    
    @Test
    void testAuthServiceCognitoIntegration() {
        // Test that AuthService has Cognito integration without actually calling AWS
        // This verifies the wiring and basic structure
        
        // The service should be properly configured
        assertNotNull(authService);
        
        // Test that invalid token handling works (doesn't throw exceptions)
        assertDoesNotThrow(() -> {
            boolean isValid = authService.isValidToken("invalid-token");
            // Should return false for invalid token when Cognito is disabled
            assertFalse(isValid);
        });
    }
    
    @Test
    void testCognitoAuthResultClass() {
        // Test the CognitoAuthResult inner class functionality
        User testUser = new User();
        testUser.setUsername("testuser");
        
        CognitoAuthenticationService.CognitoAuthResult successResult = 
            new CognitoAuthenticationService.CognitoAuthResult(
                true, testUser, "access-token", "id-token", "refresh-token", null
            );
        
        assertTrue(successResult.isSuccess());
        assertEquals(testUser, successResult.getUser());
        assertEquals("access-token", successResult.getAccessToken());
        assertEquals("id-token", successResult.getIdToken());
        assertEquals("refresh-token", successResult.getRefreshToken());
        assertNull(successResult.getErrorMessage());
        
        CognitoAuthenticationService.CognitoAuthResult failureResult = 
            new CognitoAuthenticationService.CognitoAuthResult(
                false, null, null, null, null, "Authentication failed"
            );
        
        assertFalse(failureResult.isSuccess());
        assertNull(failureResult.getUser());
        assertEquals("Authentication failed", failureResult.getErrorMessage());
    }
}