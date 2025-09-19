package com.Human.Ai.D.makit.config;

import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.domain.UserRole;
import com.Human.Ai.D.makit.service.CognitoAuthenticationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for JwtAuthenticationFilter
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {
    
    @Mock
    private CognitoAuthenticationService cognitoAuthService;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    @Mock
    private SecurityContext securityContext;
    
    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setCognitoUserId("cognito-user-123");
        testUser.setUserRole(UserRole.MARKETING_MANAGER);
        testUser.setCompanyName("Test Company");
        testUser.setActive(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());
        
        SecurityContextHolder.setContext(securityContext);
    }
    
    @Test
    void testDoFilterInternal_CognitoDisabled_ShouldPassThrough() throws ServletException, IOException {
        // Arrange
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "cognitoEnabled", false);
        
        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Assert
        verify(filterChain).doFilter(request, response);
        verify(cognitoAuthService, never()).validateToken(anyString());
        verify(securityContext, never()).setAuthentication(any());
    }
    
    @Test
    void testDoFilterInternal_CognitoEnabled_NoAuthHeader() throws ServletException, IOException {
        // Arrange
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "cognitoEnabled", true);
        when(request.getHeader("Authorization")).thenReturn(null);
        
        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Assert
        verify(filterChain).doFilter(request, response);
        verify(cognitoAuthService, never()).validateToken(anyString());
        verify(securityContext, never()).setAuthentication(any());
    }
    
    @Test
    void testDoFilterInternal_CognitoEnabled_InvalidAuthHeader() throws ServletException, IOException {
        // Arrange
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "cognitoEnabled", true);
        when(request.getHeader("Authorization")).thenReturn("Basic invalid-header");
        
        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Assert
        verify(filterChain).doFilter(request, response);
        verify(cognitoAuthService, never()).validateToken(anyString());
        verify(securityContext, never()).setAuthentication(any());
    }
    
    @Test
    void testDoFilterInternal_CognitoEnabled_ValidToken() throws ServletException, IOException {
        // Arrange
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "cognitoEnabled", true);
        String token = "valid-jwt-token";
        String authHeader = "Bearer " + token;
        
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(cognitoAuthService.validateToken(token)).thenReturn(testUser);
        when(securityContext.getAuthentication()).thenReturn(null);
        
        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Assert
        verify(filterChain).doFilter(request, response);
        verify(cognitoAuthService).validateToken(token);
        verify(securityContext).setAuthentication(any(Authentication.class));
    }
    
    @Test
    void testDoFilterInternal_CognitoEnabled_InvalidToken() throws ServletException, IOException {
        // Arrange
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "cognitoEnabled", true);
        String token = "invalid-jwt-token";
        String authHeader = "Bearer " + token;
        
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(cognitoAuthService.validateToken(token)).thenReturn(null);
        
        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Assert
        verify(filterChain).doFilter(request, response);
        verify(cognitoAuthService).validateToken(token);
        verify(securityContext, never()).setAuthentication(any());
    }
    
    @Test
    void testDoFilterInternal_CognitoEnabled_ExistingAuthentication() throws ServletException, IOException {
        // Arrange
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "cognitoEnabled", true);
        String token = "valid-jwt-token";
        String authHeader = "Bearer " + token;
        
        Authentication existingAuth = mock(Authentication.class);
        
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(cognitoAuthService.validateToken(token)).thenReturn(testUser);
        when(securityContext.getAuthentication()).thenReturn(existingAuth);
        
        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Assert
        verify(filterChain).doFilter(request, response);
        verify(cognitoAuthService).validateToken(token);
        // Should not set authentication if one already exists
        verify(securityContext, never()).setAuthentication(any());
    }
    
    @Test
    void testDoFilterInternal_CognitoEnabled_TokenValidationException() throws ServletException, IOException {
        // Arrange
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "cognitoEnabled", true);
        String token = "problematic-jwt-token";
        String authHeader = "Bearer " + token;
        
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(cognitoAuthService.validateToken(token)).thenThrow(new RuntimeException("Token validation error"));
        
        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Assert
        verify(filterChain).doFilter(request, response);
        verify(cognitoAuthService).validateToken(token);
        verify(securityContext, never()).setAuthentication(any());
    }
    
    @Test
    void testDoFilterInternal_CognitoEnabled_UserRoleMapping() throws ServletException, IOException {
        // Arrange
        ReflectionTestUtils.setField(jwtAuthenticationFilter, "cognitoEnabled", true);
        String token = "valid-jwt-token";
        String authHeader = "Bearer " + token;
        
        // Test different user roles
        testUser.setUserRole(UserRole.ADMIN);
        
        when(request.getHeader("Authorization")).thenReturn(authHeader);
        when(cognitoAuthService.validateToken(token)).thenReturn(testUser);
        when(securityContext.getAuthentication()).thenReturn(null);
        
        // Act
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);
        
        // Assert
        verify(filterChain).doFilter(request, response);
        verify(cognitoAuthService).validateToken(token);
        verify(securityContext).setAuthentication(argThat(auth -> {
            return auth.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        }));
    }
}