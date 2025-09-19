package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.domain.UserRole;
import com.Human.Ai.D.makit.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for AWS Cognito authentication integration
 */
@Service
public class CognitoAuthenticationService {
    
    private static final Logger logger = LoggerFactory.getLogger(CognitoAuthenticationService.class);
    
    @Autowired
    private CognitoIdentityProviderClient cognitoClient;
    
    @Autowired
    private UserRepository userRepository;
    
    @Value("${aws.cognito.user-pool-id:us-east-1_example}")
    private String userPoolId;
    
    @Value("${aws.cognito.client-id:example-client-id}")
    private String clientId;
    
    @Value("${jwt.secret:mySecretKey}")
    private String jwtSecret;
    
    /**
     * Authenticate user with Cognito
     */
    public CognitoAuthResult authenticateUser(String username, String password) {
        try {
            // Create authentication request
            Map<String, String> authParameters = new HashMap<>();
            authParameters.put("USERNAME", username);
            authParameters.put("PASSWORD", password);
            
            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                    .clientId(clientId)
                    .authParameters(authParameters)
                    .build();
            
            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);
            
            if (authResponse.authenticationResult() != null) {
                AuthenticationResultType authResult = authResponse.authenticationResult();
                String accessToken = authResult.accessToken();
                String idToken = authResult.idToken();
                String refreshToken = authResult.refreshToken();
                
                // Get user attributes from Cognito
                GetUserRequest getUserRequest = GetUserRequest.builder()
                        .accessToken(accessToken)
                        .build();
                
                GetUserResponse getUserResponse = cognitoClient.getUser(getUserRequest);
                String cognitoUserId = getUserResponse.username();
                
                // Sync user with local database
                User user = syncUserWithCognito(cognitoUserId, getUserResponse.userAttributes());
                
                return new CognitoAuthResult(true, user, accessToken, idToken, refreshToken, null);
            } else {
                return new CognitoAuthResult(false, null, null, null, null, "Authentication failed");
            }
            
        } catch (NotAuthorizedException e) {
            logger.warn("Authentication failed for user: {}", username);
            return new CognitoAuthResult(false, null, null, null, null, "Invalid credentials");
        } catch (Exception e) {
            logger.error("Error during Cognito authentication", e);
            return new CognitoAuthResult(false, null, null, null, null, "Authentication service error");
        }
    }
    
    /**
     * Register new user in Cognito
     */
    public CognitoAuthResult registerUser(String username, String email, String password, String companyName) {
        try {
            // Create user attributes
            AttributeType emailAttr = AttributeType.builder()
                    .name("email")
                    .value(email)
                    .build();
            
            AttributeType companyAttr = AttributeType.builder()
                    .name("custom:company_name")
                    .value(companyName)
                    .build();
            
            // Create user in Cognito
            AdminCreateUserRequest createUserRequest = AdminCreateUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .userAttributes(emailAttr, companyAttr)
                    .temporaryPassword(password)
                    .messageAction(MessageActionType.SUPPRESS) // Don't send welcome email
                    .build();
            
            AdminCreateUserResponse createUserResponse = cognitoClient.adminCreateUser(createUserRequest);
            
            // Set permanent password
            AdminSetUserPasswordRequest setPasswordRequest = AdminSetUserPasswordRequest.builder()
                    .userPoolId(userPoolId)
                    .username(username)
                    .password(password)
                    .permanent(true)
                    .build();
            
            cognitoClient.adminSetUserPassword(setPasswordRequest);
            
            // Create local user record
            User user = new User(username, email, "", companyName);
            user.setCognitoUserId(username);
            user.setUserRole(UserRole.VIEWER); // Default role
            user = userRepository.save(user);
            
            // Authenticate the newly created user
            return authenticateUser(username, password);
            
        } catch (UsernameExistsException e) {
            logger.warn("User already exists: {}", username);
            return new CognitoAuthResult(false, null, null, null, null, "User already exists");
        } catch (Exception e) {
            logger.error("Error during user registration", e);
            return new CognitoAuthResult(false, null, null, null, null, "Registration failed");
        }
    }
    
    /**
     * Validate JWT token from Cognito
     */
    public User validateToken(String token) {
        try {
            // In a real implementation, you would validate the JWT signature
            // using Cognito's public keys. For now, we'll use a simple approach.
            
            // Remove Bearer prefix if present
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            // Parse JWT token (simplified - in production use proper JWT validation)
            Claims claims = parseJwtToken(token);
            String cognitoUserId = claims.getSubject();
            
            // Find user by Cognito ID
            Optional<User> userOpt = userRepository.findByCognitoUserId(cognitoUserId);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);
                return user;
            }
            
            return null;
        } catch (Exception e) {
            logger.error("Error validating token", e);
            return null;
        }
    }
    
    /**
     * Refresh access token using refresh token
     */
    public CognitoAuthResult refreshToken(String refreshToken) {
        try {
            Map<String, String> authParameters = new HashMap<>();
            authParameters.put("REFRESH_TOKEN", refreshToken);
            
            InitiateAuthRequest authRequest = InitiateAuthRequest.builder()
                    .authFlow(AuthFlowType.REFRESH_TOKEN_AUTH)
                    .clientId(clientId)
                    .authParameters(authParameters)
                    .build();
            
            InitiateAuthResponse authResponse = cognitoClient.initiateAuth(authRequest);
            
            if (authResponse.authenticationResult() != null) {
                AuthenticationResultType authResult = authResponse.authenticationResult();
                String newAccessToken = authResult.accessToken();
                String newIdToken = authResult.idToken();
                
                // Get user from new token
                User user = validateToken(newAccessToken);
                
                return new CognitoAuthResult(true, user, newAccessToken, newIdToken, refreshToken, null);
            }
            
            return new CognitoAuthResult(false, null, null, null, null, "Token refresh failed");
            
        } catch (Exception e) {
            logger.error("Error refreshing token", e);
            return new CognitoAuthResult(false, null, null, null, null, "Token refresh error");
        }
    }
    
    /**
     * Sign out user from Cognito
     */
    public void signOutUser(String accessToken) {
        try {
            GlobalSignOutRequest signOutRequest = GlobalSignOutRequest.builder()
                    .accessToken(accessToken)
                    .build();
            
            cognitoClient.globalSignOut(signOutRequest);
        } catch (Exception e) {
            logger.error("Error signing out user", e);
        }
    }
    
    /**
     * Sync user data from Cognito to local database
     */
    private User syncUserWithCognito(String cognitoUserId, java.util.List<AttributeType> attributes) {
        Optional<User> existingUser = userRepository.findByCognitoUserId(cognitoUserId);
        
        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            user = new User();
            user.setCognitoUserId(cognitoUserId);
            user.setUserRole(UserRole.VIEWER); // Default role
        }
        
        // Update user attributes from Cognito
        for (AttributeType attr : attributes) {
            switch (attr.name()) {
                case "email":
                    user.setEmail(attr.value());
                    break;
                case "custom:company_name":
                    user.setCompanyName(attr.value());
                    break;
                case "preferred_username":
                    user.setUsername(attr.value());
                    break;
            }
        }
        
        // Set username to cognitoUserId if not set
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            user.setUsername(cognitoUserId);
        }
        
        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    /**
     * Parse JWT token (simplified implementation)
     */
    private Claims parseJwtToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    
    /**
     * Result class for Cognito authentication operations
     */
    public static class CognitoAuthResult {
        private final boolean success;
        private final User user;
        private final String accessToken;
        private final String idToken;
        private final String refreshToken;
        private final String errorMessage;
        
        public CognitoAuthResult(boolean success, User user, String accessToken, String idToken, 
                               String refreshToken, String errorMessage) {
            this.success = success;
            this.user = user;
            this.accessToken = accessToken;
            this.idToken = idToken;
            this.refreshToken = refreshToken;
            this.errorMessage = errorMessage;
        }
        
        // Getters
        public boolean isSuccess() { return success; }
        public User getUser() { return user; }
        public String getAccessToken() { return accessToken; }
        public String getIdToken() { return idToken; }
        public String getRefreshToken() { return refreshToken; }
        public String getErrorMessage() { return errorMessage; }
    }
}