package com.xiongdwm.ai_demo.webapp.resource;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.xiongdwm.ai_demo.utils.global.ApiResponse;
import com.xiongdwm.ai_demo.webapp.bo.LoginRequest;
import com.xiongdwm.ai_demo.webapp.bo.RegisterRequest;
import com.xiongdwm.ai_demo.webapp.entities.AiSysUser;
import com.xiongdwm.ai_demo.webapp.entities.KnowledgeBase;
import com.xiongdwm.ai_demo.webapp.service.AuthService;

@RestController
public class AuthController {
    @Autowired
    private AuthService authService;


    @PostMapping("/web/login")
    public ApiResponse<String> login(@RequestBody LoginRequest user) {
        try {
            boolean loginStatus=authService.login(user.getUsername(), user.getPassword());
            if(!loginStatus) return ApiResponse.error("密码错误");
            Long expire = System.currentTimeMillis() + 3600000; 
            String token = user.getUsername()+"-"+ expire;
            return ApiResponse.success(token);
        } catch (RuntimeException e) {
            return ApiResponse.error("登录失败");
        }
    }
    @PostMapping("/auth/verifyKnowledge")
    public ApiResponse<String> verifyKnowledge(@RequestParam Long knowledge,@RequestHeader("Authorization") String token) {
        String username = token.split("-")[0];
        if (username == null || username.isEmpty()) {
            return ApiResponse.error("无效的token");
        }
        boolean hasPermission = authService.hasPermission(knowledge, username);
        if(!hasPermission) return ApiResponse.error("没有权限访问该知识库");
        
        return ApiResponse.success("权限验证通过");

    }
    @PostMapping("/user/list")
    public ApiResponse<List<AiSysUser>> getAllUsers() {
        List<AiSysUser> users = authService.getAllUsers();
        if(users == null || users.isEmpty()) return ApiResponse.error(Collections.emptyList());
        return ApiResponse.success(users);
    }
    @PostMapping("/user/register")
    public ApiResponse<String> registerUser(@RequestBody RegisterRequest user) {
        if (user.getUsername() == null || user.getPassword() == null) {
            return ApiResponse.error("用户名或密码不能为空");
        }
        boolean registrationStatus = authService.registerUser(user.getUsername(), user.getPassword(),user.getRealName());
        if (!registrationStatus) {
            return ApiResponse.error("注册失败");
        }
        return ApiResponse.success("注册成功");
    }

    @PostMapping("/knowledge/getAuthorized")
    public ApiResponse<Object> getAuthorizedKnowledgeBases(@RequestHeader("Authorization") String token) {
        String username = token.split("-")[0];
        if (username == null || username.isEmpty()) {
            return ApiResponse.error("无效的token");
        }
        List<KnowledgeBase> knowledgeBases = authService.getAllUsers().stream()
                .filter(user -> user.getUsername().equals(username))
                .flatMap(user -> user.getKnowledgeBases().stream())
                .toList();
        return ApiResponse.success(knowledgeBases);
    }
}
