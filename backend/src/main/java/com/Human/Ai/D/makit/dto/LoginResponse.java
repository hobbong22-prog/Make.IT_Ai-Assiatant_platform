package com.Human.Ai.D.makit.dto;

import com.Human.Ai.D.makit.domain.User;

public class LoginResponse {
    
    private User user;
    private String token;
    private String message;
    
    // Constructors
    public LoginResponse() {}
    
    public LoginResponse(User user, String token, String message) {
        this.user = user;
        this.token = token;
        this.message = message;
    }
    
    // Getters and Setters
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}