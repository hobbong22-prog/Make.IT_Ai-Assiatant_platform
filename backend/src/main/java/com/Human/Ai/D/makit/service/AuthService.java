package com.Human.Ai.D.makit.service;

import com.Human.Ai.D.makit.domain.User;
import com.Human.Ai.D.makit.domain.UserRole;
import com.Human.Ai.D.makit.dto.LoginResponse;
import com.Human.Ai.D.makit.dto.RegisterRequest;
import com.Human.Ai.D.makit.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private CognitoAuthenticationService cognitoAuthService;
    
    @Value("${aws.cognito.enabled:false}")
    private boolean cognitoEnabled;
    
    // 간단한 토큰 저장소 (실제 운영에서는 Redis나 JWT 사용)
    private Map<String, User> tokenStore = new HashMap<>();
    
    public LoginResponse login(String email, String password) {
        if (cognitoEnabled) {
            return loginWithCognito(email, password);
        } else {
            return loginWithLocalAuth(email, password);
        }
    }
    
    private LoginResponse loginWithCognito(String email, String password) {
        // For Cognito, we use email as username
        CognitoAuthenticationService.CognitoAuthResult result = 
            cognitoAuthService.authenticateUser(email, password);
        
        if (result.isSuccess()) {
            User user = result.getUser();
            // Clean up user object for response
            user.setPassword(null);
            user.setCampaigns(null);
            user.setContents(null);
            
            return new LoginResponse(user, result.getAccessToken(), "로그인 성공");
        } else {
            throw new RuntimeException(result.getErrorMessage());
        }
    }
    
    private LoginResponse loginWithLocalAuth(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }
        
        User user = userOpt.get();
        
        // BCrypt를 사용한 비밀번호 검증
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }
        
        // 간단한 토큰 생성
        String token = generateToken(user);
        tokenStore.put(token, user);
        
        // 비밀번호는 응답에서 제거하고 순환 참조 방지를 위해 campaigns도 제거
        user.setPassword(null);
        user.setCampaigns(null);
        user.setContents(null);
        
        return new LoginResponse(user, token, "로그인 성공");
    }
    
    public LoginResponse register(RegisterRequest request) {
        if (cognitoEnabled) {
            return registerWithCognito(request);
        } else {
            return registerWithLocalAuth(request);
        }
    }
    
    private LoginResponse registerWithCognito(RegisterRequest request) {
        CognitoAuthenticationService.CognitoAuthResult result = 
            cognitoAuthService.registerUser(
                request.getUsername(), 
                request.getEmail(), 
                request.getPassword(), 
                request.getCompanyName()
            );
        
        if (result.isSuccess()) {
            User user = result.getUser();
            // Clean up user object for response
            user.setPassword(null);
            user.setCampaigns(null);
            user.setContents(null);
            
            return new LoginResponse(user, result.getAccessToken(), "회원가입 성공");
        } else {
            throw new RuntimeException(result.getErrorMessage());
        }
    }
    
    private LoginResponse registerWithLocalAuth(RegisterRequest request) {
        // 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }
        
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("이미 존재하는 사용자명입니다.");
        }
        
        // 새 사용자 생성 (비밀번호 암호화)
        User user = new User(
            request.getUsername(),
            request.getEmail(),
            passwordEncoder.encode(request.getPassword()),
            request.getCompanyName()
        );
        user.setUserRole(UserRole.VIEWER); // Default role for new users
        
        user = userRepository.save(user);
        
        // 토큰 생성
        String token = generateToken(user);
        tokenStore.put(token, user);
        
        // 비밀번호는 응답에서 제거하고 순환 참조 방지를 위해 campaigns도 제거
        user.setPassword(null);
        user.setCampaigns(null);
        user.setContents(null);
        
        return new LoginResponse(user, token, "회원가입 성공");
    }
    
    public User getCurrentUser(String token) {
        if (cognitoEnabled) {
            return getCurrentUserFromCognito(token);
        } else {
            return getCurrentUserFromLocal(token);
        }
    }
    
    private User getCurrentUserFromCognito(String token) {
        User user = cognitoAuthService.validateToken(token);
        if (user == null) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }
        
        // Clean up user object for response
        user.setPassword(null);
        user.setCampaigns(null);
        user.setContents(null);
        return user;
    }
    
    private User getCurrentUserFromLocal(String token) {
        // Bearer 토큰에서 실제 토큰 추출
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        User user = tokenStore.get(token);
        if (user == null) {
            throw new RuntimeException("유효하지 않은 토큰입니다.");
        }
        
        // 최신 사용자 정보 조회
        Optional<User> currentUser = userRepository.findById(user.getId());
        if (currentUser.isEmpty()) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }
        
        User result = currentUser.get();
        result.setPassword(null); // 비밀번호 제거
        result.setCampaigns(null); // 순환 참조 방지
        result.setContents(null); // 순환 참조 방지
        return result;
    }
    
    public void logout(String token) {
        if (cognitoEnabled) {
            cognitoAuthService.signOutUser(token);
        } else {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            tokenStore.remove(token);
        }
    }
    
    private String generateToken(User user) {
        // 간단한 토큰 생성 (실제 운영에서는 JWT 사용)
        String tokenData = user.getId() + ":" + user.getEmail() + ":" + System.currentTimeMillis();
        return Base64.getEncoder().encodeToString(tokenData.getBytes());
    }
    
    public boolean isValidToken(String token) {
        if (cognitoEnabled) {
            User user = cognitoAuthService.validateToken(token);
            return user != null;
        } else {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            return tokenStore.containsKey(token);
        }
    }

    /**
     * Update user role (Admin only operation)
     */
    public User updateUserRole(Long userId, UserRole newRole) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found with ID: " + userId);
        }

        User user = userOpt.get();
        user.setUserRole(newRole);
        return userRepository.save(user);
    }

    /**
     * Update user active status (Admin only operation)
     */
    public User updateUserStatus(Long userId, boolean isActive) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found with ID: " + userId);
        }

        User user = userOpt.get();
        user.setActive(isActive);
        return userRepository.save(user);
    }

    /**
     * Find user by ID
     */
    public User findById(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return null;
        }
        
        User user = userOpt.get();
        user.setPassword(null); // Remove password from response
        return user;
    }

    /**
     * Get all users (Admin only operation)
     */
    public List<User> getAllUsers() {
        List<User> users = userRepository.findAll();
        // Remove passwords from all users
        users.forEach(user -> {
            user.setPassword(null);
            user.setCampaigns(null); // Prevent circular references
            user.setContents(null); // Prevent circular references
        });
        return users;
    }

    /**
     * Check if the current authenticated user is the same as the given user ID
     */
    public boolean isCurrentUser(Long userId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return false;
            }

            // In a real implementation, you would extract user ID from JWT or session
            // For now, we'll use a simple token-based approach
            String token = (String) auth.getCredentials();
            User currentUser = getCurrentUser(token);
            return currentUser != null && currentUser.getId().equals(userId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Update user's last login time
     */
    public void updateLastLogin(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }

    /**
     * Check if user has specific role
     */
    public boolean hasRole(Long userId, UserRole role) {
        User user = findById(userId);
        return user != null && user.getUserRole() == role;
    }

    /**
     * Check if user can perform specific action based on role
     */
    public boolean canManageContent(Long userId) {
        User user = findById(userId);
        return user != null && user.getUserRole().canManageContent();
    }

    public boolean canManageCampaigns(Long userId) {
        User user = findById(userId);
        return user != null && user.getUserRole().canManageCampaigns();
    }

    public boolean canAccessAnalytics(Long userId) {
        User user = findById(userId);
        return user != null && user.getUserRole().canAccessAnalytics();
    }

    public boolean canManageUsers(Long userId) {
        User user = findById(userId);
        return user != null && user.getUserRole().canManageUsers();
    }
}