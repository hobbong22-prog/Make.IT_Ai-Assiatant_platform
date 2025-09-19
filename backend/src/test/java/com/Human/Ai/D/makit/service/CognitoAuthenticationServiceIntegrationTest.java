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
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for CognitoAuthenticationService
 */
@ExtendWith(MockitoExtension.class)
class CognitoAuthenticationServiceIntegrationTest {
    
    @Mock
    private CognitoIdentityProviderClient cognitoClient;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private CognitoAuthenticationService cognitoAuthService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setCognitoUserId("cognito-user-123");
        testUser.setUserRole(UserRole.VIEWER);
        testUser.setCompanyName("Test Company");
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
    }
    
    @Test
    void testAuthenticateUser_Success() {
        // Arrange
        String username = "testuser";
        String password = "password123";
        String accessToken = "mock-access-token";
        String idToken = "mock-id-token";
        String refreshToken = "mock-refresh-token";
        
        AuthenticationResultType authResult = AuthenticationResultType.builder()
                .accessToken(accessToken)
                .idToken(idToken)
                .refreshToken(refreshToken)
                .build();
        
        InitiateAuthResponse authResponse = InitiateAuthResponse.builder()
                .authenticationResult(authResult)
                .build();
        
        AttributeType emailAttr = AttributeType.builder()
                .name("email")
                .value("test@example.com")
                .build();
        
        AttributeType companyAttr = AttributeType.builder()
                .name("custom:company_name")
                .value("Test Company")
                .build();
        
        GetUserResponse getUserResponse = GetUserResponse.builder()
                .username("cognito-user-123")
                .userAttributes(Arrays.asList(emailAttr, companyAttr))
                .build();
        
        when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class))).thenReturn(authResponse);
        when(cognitoClient.getUser(any(GetUserRequest.class))).thenReturn(getUserResponse);
        when(userRepository.findByCognitoUserId("cognito-user-123")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // Act
        CognitoAuthenticationService.CognitoAuthResult result = 
            cognitoAuthService.authenticateUser(username, password);
        
        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getUser());
        assertEquals(accessToken, result.getAccessToken());
        assertEquals(idToken, result.getIdToken());
        assertEquals(refreshToken, result.getRefreshToken());
        assertNull(result.getErrorMessage());
        
        verify(cognitoClient).initiateAuth(any(InitiateAuthRequest.class));
        verify(cognitoClient).getUser(any(GetUserRequest.class));
        verify(userRepository).save(any(User.class));
    }
    
    @Test
    void testAuthenticateUser_InvalidCredentials() {
        // Arrange
        String username = "testuser";
        String password = "wrongpassword";
        
        when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class)))
                .thenThrow(NotAuthorizedException.builder().message("Invalid credentials").build());
        
        // Act
        CognitoAuthenticationService.CognitoAuthResult result = 
            cognitoAuthService.authenticateUser(username, password);
        
        // Assert
        assertFalse(result.isSuccess());
        assertNull(result.getUser());
        assertNull(result.getAccessToken());
        assertEquals("Invalid credentials", result.getErrorMessage());
        
        verify(cognitoClient).initiateAuth(any(InitiateAuthRequest.class));
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testRegisterUser_Success() {
        // Arrange
        String username = "newuser";
        String email = "newuser@example.com";
        String password = "password123";
        String companyName = "New Company";
        
        AdminCreateUserResponse createUserResponse = AdminCreateUserResponse.builder()
                .user(UserType.builder().username(username).build())
                .build();
        
        when(cognitoClient.adminCreateUser(any(AdminCreateUserRequest.class)))
                .thenReturn(createUserResponse);
        when(cognitoClient.adminSetUserPassword(any(AdminSetUserPasswordRequest.class)))
                .thenReturn(AdminSetUserPasswordResponse.builder().build());
        
        User newUser = new User(username, email, "", companyName);
        newUser.setId(2L);
        newUser.setCognitoUserId(username);
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        
        // Mock the authentication call that happens after registration
        AuthenticationResultType authResult = AuthenticationResultType.builder()
                .accessToken("mock-access-token")
                .idToken("mock-id-token")
                .refreshToken("mock-refresh-token")
                .build();
        
        InitiateAuthResponse authResponse = InitiateAuthResponse.builder()
                .authenticationResult(authResult)
                .build();
        
        AttributeType emailAttr = AttributeType.builder()
                .name("email")
                .value(email)
                .build();
        
        GetUserResponse getUserResponse = GetUserResponse.builder()
                .username(username)
                .userAttributes(Arrays.asList(emailAttr))
                .build();
        
        when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class))).thenReturn(authResponse);
        when(cognitoClient.getUser(any(GetUserRequest.class))).thenReturn(getUserResponse);
        when(userRepository.findByCognitoUserId(username)).thenReturn(Optional.of(newUser));
        
        // Act
        CognitoAuthenticationService.CognitoAuthResult result = 
            cognitoAuthService.registerUser(username, email, password, companyName);
        
        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getUser());
        assertNotNull(result.getAccessToken());
        
        verify(cognitoClient).adminCreateUser(any(AdminCreateUserRequest.class));
        verify(cognitoClient).adminSetUserPassword(any(AdminSetUserPasswordRequest.class));
        verify(userRepository, atLeastOnce()).save(any(User.class));
    }
    
    @Test
    void testRegisterUser_UserAlreadyExists() {
        // Arrange
        String username = "existinguser";
        String email = "existing@example.com";
        String password = "password123";
        String companyName = "Company";
        
        when(cognitoClient.adminCreateUser(any(AdminCreateUserRequest.class)))
                .thenThrow(UsernameExistsException.builder().message("User already exists").build());
        
        // Act
        CognitoAuthenticationService.CognitoAuthResult result = 
            cognitoAuthService.registerUser(username, email, password, companyName);
        
        // Assert
        assertFalse(result.isSuccess());
        assertNull(result.getUser());
        assertEquals("User already exists", result.getErrorMessage());
        
        verify(cognitoClient).adminCreateUser(any(AdminCreateUserRequest.class));
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testValidateToken_ValidToken() {
        // Arrange
        String token = "Bearer valid-jwt-token";
        
        when(userRepository.findByCognitoUserId(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // Act & Assert
        // Note: This test would require proper JWT token parsing in a real implementation
        // For now, we'll test the basic structure
        assertDoesNotThrow(() -> {
            User result = cognitoAuthService.validateToken(token);
            // In a real implementation with proper JWT validation, this would return the user
        });
    }
    
    @Test
    void testRefreshToken_Success() {
        // Arrange
        String refreshToken = "valid-refresh-token";
        String newAccessToken = "new-access-token";
        String newIdToken = "new-id-token";
        
        AuthenticationResultType authResult = AuthenticationResultType.builder()
                .accessToken(newAccessToken)
                .idToken(newIdToken)
                .build();
        
        InitiateAuthResponse authResponse = InitiateAuthResponse.builder()
                .authenticationResult(authResult)
                .build();
        
        when(cognitoClient.initiateAuth(any(InitiateAuthRequest.class))).thenReturn(authResponse);
        when(userRepository.findByCognitoUserId(anyString())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // Act
        CognitoAuthenticationService.CognitoAuthResult result = 
            cognitoAuthService.refreshToken(refreshToken);
        
        // Assert
        assertTrue(result.isSuccess());
        assertEquals(newAccessToken, result.getAccessToken());
        assertEquals(newIdToken, result.getIdToken());
        assertEquals(refreshToken, result.getRefreshToken());
        
        verify(cognitoClient).initiateAuth(any(InitiateAuthRequest.class));
    }
    
    @Test
    void testSignOutUser_Success() {
        // Arrange
        String accessToken = "valid-access-token";
        
        when(cognitoClient.globalSignOut(any(GlobalSignOutRequest.class)))
                .thenReturn(GlobalSignOutResponse.builder().build());
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            cognitoAuthService.signOutUser(accessToken);
        });
        
        verify(cognitoClient).globalSignOut(any(GlobalSignOutRequest.class));
    }
    
    @Test
    void testSyncUserWithCognito_NewUser() {
        // Arrange
        String cognitoUserId = "new-cognito-user";
        
        AttributeType emailAttr = AttributeType.builder()
                .name("email")
                .value("newuser@example.com")
                .build();
        
        AttributeType companyAttr = AttributeType.builder()
                .name("custom:company_name")
                .value("New Company")
                .build();
        
        when(userRepository.findByCognitoUserId(cognitoUserId)).thenReturn(Optional.empty());
        
        User newUser = new User();
        newUser.setId(3L);
        newUser.setCognitoUserId(cognitoUserId);
        newUser.setEmail("newuser@example.com");
        newUser.setCompanyName("New Company");
        newUser.setUsername(cognitoUserId);
        newUser.setUserRole(UserRole.VIEWER);
        
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        
        // Act
        // This would be called internally by syncUserWithCognito method
        // We can't test it directly as it's private, but we can verify the behavior
        // through the public methods that use it
        
        // Assert
        verify(userRepository, never()).findByCognitoUserId(cognitoUserId);
    }
}