package com.xiongdwm.ai_demo.webapp.service;

import java.util.List;

import com.xiongdwm.ai_demo.webapp.entities.AiSysUser;

public interface AuthService {
    /**
     * 检查用户是否有权限访问知识库
     * @param knowledgeBaseId 知识库ID
     * @return true 如果用户有权限访问知识库，否则返回 false
     */
    boolean hasPermission(Long knowledgeBaseId, String username);

    boolean registerUser(String username, String password,String realName);

    boolean login(String username,String password);

    List<AiSysUser> getAllUsers();


}
