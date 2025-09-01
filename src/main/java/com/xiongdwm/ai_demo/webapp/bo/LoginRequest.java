package com.xiongdwm.ai_demo.webapp.bo;

public record LoginRequest(
    String username,
    String password
) {
    public LoginRequest {
        if (username == null || password == null) {
            throw new IllegalArgumentException("Username and password must not be null");
        }
    }
    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
} 
