package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.domain.UserRole;
import com.Human.Ai.D.makit.dto.LoginResponse;
import com.Human.Ai.D.makit.dto.RegisterRequest;
import com.Human.Ai.D.makit.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for AuthService with Cognito integration
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceCognitoIntegrationTest {
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private CognitoAuthenticationService cognitoAuthService;
    
    @InjectMocks
    private AuthService authService;
    
    private User testUser;
    private RegisterRequest registerRequest;
    
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
        
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setCompanyName("New Company");
    }
    
    @Test
    void testLogin_WithCognitoEnabled_Success() {
        // Arrange
        ReflectionTestUtils.setField(authService, "cognitoEnabled", true);
        
        String email = "test@example.com";
        String password = "password123";
        String accessToken = "mock-access-token";
        
        CognitoAuthenticationService.CognitoAuthResult cognitoResult = 
            new CognitoAuthenticationService.CognitoAuthResult(
                true, testUser, accessToken, "id-token", "refresh-token", null
            );
        
        when(cognitoAuthService.authenticateUser(email, password)).thenReturn(cognitoResult);
        
        // Act
        LoginResponse response = authService.login(email, password);
        
        // Assert
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getUser().getId());
        assertEquals(accessToken, response.getToken());
        assertEquals("로그인 성공", response.getMessage());
        assertNull(response.getUser().getPassword());
        assertNull(response.getUser().getCampaigns());
        assertNull(response.getUser().getContents());
        
        verify(cognitoAuthService).authenticateUser(email, password);
    }
    
    @Test
    void testLogin_WithCognitoEnabled_Failure() {
        // Arrange
        ReflectionTestUtils.setField(authService, "cognitoEnabled", true);
        
        String email = "test@example.com";
        String password = "wrongpassword";
        
        CognitoAuthenticationService.CognitoAuthResult cognitoResult = 
            new CognitoAuthenticationService.CognitoAuthResult(
                false, null, null, null, null, "Invalid credentials"
            );
        
        when(cognitoAuthService.authenticateUser(email, password)).thenReturn(cognitoResult);
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(email, password);
        });
        
        assertEquals("Invalid credentials", exception.getMessage());
        verify(cognitoAuthService).authenticateUser(email, password);
    }
    
    @Test
    void testLogin_WithCognitoDisabled_Success() {
        // Arrange
        ReflectionTestUtils.setField(authService, "cognitoEnabled", false);
        
        String email = "test@example.com";
        String password = "password123";
        
        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(testUser));
        when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);
        
        // Act
        LoginResponse response = authService.login(email, password);
        
        // Assert
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getUser().getId());
        assertNotNull(response.getToken());
        assertEquals("로그인 성공", response.getMessage());
        
        verify(userRepository).findByEmail(email);
        verify(passwordEncoder).matches(password, testUser.getPassword());
        verify(cognitoAuthService, never()).authenticateUser(anyString(), anyString());
    }
    
    @Test
    void testRegister_WithCognitoEnabled_Success() {
        // Arrange
        ReflectionTestUtils.setField(authService, "cognitoEnabled", true);
        
        String accessToken = "mock-access-token";
        
        CognitoAuthenticationService.CognitoAuthResult cognitoResult = 
            new CognitoAuthenticationService.CognitoAuthResult(
                true, testUser, accessToken, "id-token", "refresh-token", null
            );
        
        when(cognitoAuthService.registerUser(
            registerRequest.getUsername(),
            registerRequest.getEmail(),
            registerRequest.getPassword(),
            registerRequest.getCompanyName()
        )).thenReturn(cognitoResult);
        
        // Act
        LoginResponse response = authService.register(registerRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getUser().getId());
        assertEquals(accessToken, response.getToken());
        assertEquals("회원가입 성공", response.getMessage());
        
        verify(cognitoAuthService).registerUser(
            registerRequest.getUsername(),
            registerRequest.getEmail(),
            registerRequest.getPassword(),
            registerRequest.getCompanyName()
        );
    }
    
    @Test
    void testRegister_WithCognitoEnabled_Failure() {
        // Arrange
        ReflectionTestUtils.setField(authService, "cognitoEnabled", true);
        
        CognitoAuthenticationService.CognitoAuthResult cognitoResult = 
            new CognitoAuthenticationService.CognitoAuthResult(
                false, null, null, null, null, "User already exists"
            );
        
        when(cognitoAuthService.registerUser(
            registerRequest.getUsername(),
            registerRequest.getEmail(),
            registerRequest.getPassword(),
            registerRequest.getCompanyName()
        )).thenReturn(cognitoResult);
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(registerRequest);
        });
        
        assertEquals("User already exists", exception.getMessage());
        verify(cognitoAuthService).registerUser(
            registerRequest.getUsername(),
            registerRequest.getEmail(),
            registerRequest.getPassword(),
            registerRequest.getCompanyName()
        );
    }
    
    @Test
    void testRegister_WithCognitoDisabled_Success() {
        // Arrange
        ReflectionTestUtils.setField(authService, "cognitoEnabled", false);
        
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        // Act
        LoginResponse response = authService.register(registerRequest);
        
        // Assert
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getUser().getId());
        assertNotNull(response.getToken());
        assertEquals("회원가입 성공", response.getMessage());
        
        verify(userRepository).existsByEmail(registerRequest.getEmail());
        verify(userRepository).existsByUsername(registerRequest.getUsername());
        verify(passwordEncoder).encode(registerRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(cognitoAuthService, never()).registerUser(anyString(), anyString(), anyString(), anyString());
    }
    
    @Test
    void testGetCurrentUser_WithCognitoEnabled_Success() {
        // Arrange
        ReflectionTestUtils.setField(authService, "cognitoEnabled", true);
        
        String token = "Bearer cognito-jwt-token";
        
        when(cognitoAuthService.validateToken(token)).thenReturn(testUser);
        
        // Act
        User result = authService.getCurrentUser(token);
        
        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertNull(result.getPassword());
        assertNull(result.getCampaigns());
        assertNull(result.getContents());
        
        verify(cognitoAuthService).validateToken(token);
    }
    
    @Test
    void testGetCurrentUser_WithCognitoEnabled_InvalidToken() {
        // Arrange
        ReflectionTestUtils.setField(authService, "cognitoEnabled", true);
        
        String token = "Bearer invalid-token";
        
        when(cognitoAuthService.validateToken(token)).thenReturn(null);
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.getCurrentUser(token);
        });
        
        assertEquals("유효하지 않은 토큰입니다.", exception.getMessage());
        verify(cognitoAuthService).validateToken(token);
    }
    
    @Test
    void testGetCurrentUser_WithCognitoDisabled_Success() {
        // Arrange
        ReflectionTestUtils.setField(authService, "cognitoEnabled", false);
        
        String token = "Bearer local-token";
        
        // Mock the token store behavior (this would be set up during login)
        ReflectionTestUtils.setField(authService, "tokenStore", 
            java.util.Map.of("local-token", testUser));
        
        when(userRepository.findById(testUser.getId())).thenReturn(java.util.Optional.of(testUser));
        
        // Act
        User result = authService.getCurrentUser(token);
        
        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertNull(result.getPassword());
        
        verify(userRepository).findById(testUser.getId());
        verify(cognitoAuthService, never()).validateToken(anyString());
    }
    
    @Test
    void testLogout_WithCognitoEnabled() {
        // Arrange
        ReflectionTestUtils.setField(authService, "cognitoEnabled", true);
        
        String token = "Bearer cognito-token";
        
        // Act
        authService.logout(token);
        
        // Assert
        verify(cognitoAuthService).signOutUser(token);
    }
    
    @Test
    void testLogout_WithCognitoDisabled() {
        // Arrange
        ReflectionTestUtils.setField(authService, "cognitoEnabled", false);
        
        String token = "Bearer local-token";
        
        // Mock the token store
        java.util.Map<String, User> tokenStore = new java.util.HashMap<>();
        tokenStore.put("local-token", testUser);
        ReflectionTestUtils.setField(authService, "tokenStore", tokenStore);
        
        // Act
        authService.logout(token);
        
        // Assert
        assertFalse(tokenStore.containsKey("local-token"));
        verify(cognitoAuthService, never()).signOutUser(anyString());
    }
    
    @Test
    void testIsValidToken_WithCognitoEnabled_ValidToken() {
        // Arrange
        ReflectionTestUtils.setField(authService, "cognitoEnabled", true);
        
        String token = "Bearer valid-cognito-token";
        
        when(cognitoAuthService.validateToken(token)).thenReturn(testUser);
        
        // Act
        boolean result = authService.isValidToken(token);
        
        // Assert
        assertTrue(result);
        verify(cognitoAuthService).validateToken(token);
    }
    
    @Test
    void testIsValidToken_WithCognitoEnabled_InvalidToken() {
        // Arrange
        ReflectionTestUtils.setField(authService, "cognitoEnabled", true);
        
        String token = "Bearer invalid-cognito-token";
        
        when(cognitoAuthService.validateToken(token)).thenReturn(null);
        
        // Act
        boolean result = authService.isValidToken(token);
        
        // Assert
        assertFalse(result);
        verify(cognitoAuthService).validateToken(token);
    }
    
    @Test
    void testIsValidToken_WithCognitoDisabled_ValidToken() {
        // Arrange
        ReflectionTestUtils.setField(authService, "cognitoEnabled", false);
        
        String token = "Bearer local-token";
        
        // Mock the token store
        java.util.Map<String, User> tokenStore = new java.util.HashMap<>();
        tokenStore.put("local-token", testUser);
        ReflectionTestUtils.setField(authService, "tokenStore", tokenStore);
        
        // Act
        boolean result = authService.isValidToken(token);
        
        // Assert
        assertTrue(result);
        verify(cognitoAuthService, never()).validateToken(anyString());
    }
}