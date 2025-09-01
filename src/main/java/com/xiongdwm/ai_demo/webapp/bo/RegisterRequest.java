package com.xiongdwm.ai_demo.webapp.bo;

public class RegisterRequest {
    private String username;
    private String password;
    private String realName;

    public RegisterRequest(String username, String password, String realName) {
        if (username == null || password == null || realName == null) {
            throw new IllegalArgumentException("Username, password, and real name must not be null");
        }
        this.username = username;
        this.password = password;
        this.realName = realName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getRealName() {
        return realName;
    }
}
