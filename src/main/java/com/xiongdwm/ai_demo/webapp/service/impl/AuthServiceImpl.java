package com.xiongdwm.ai_demo.webapp.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.xiongdwm.ai_demo.webapp.entities.AiSysUser;
import com.xiongdwm.ai_demo.webapp.repository.AiSysUserRepository;
import com.xiongdwm.ai_demo.webapp.service.AuthService;

import jakarta.annotation.Resource;

@Service
public class AuthServiceImpl implements AuthService {
    @Resource
    private AiSysUserRepository aiSysUserRepository;

    @Override
    public boolean hasPermission(Long knowledgeBaseId, String username) {
        AiSysUser user = aiSysUserRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false; 
        }
       return user.getKnowledgeBases().stream().filter(it->it.getId()==knowledgeBaseId).findAny().isPresent();
    }

    @Override
    public boolean registerUser(String username, String password,String realName) {
        AiSysUser aiSysUser = new AiSysUser();
        aiSysUser.setUsername(username);
        aiSysUser.setPassword(password);
        aiSysUser.setRealName(realName);
        return aiSysUserRepository.save(aiSysUser) != null;
    }

    @Override
    public boolean login(String username, String password) {
        AiSysUser user = aiSysUserRepository.findByUsername(username).orElse(null);
        if (user != null && user.getPassword().equals(password)) return true;
        return false;
    }

    @Override
    public List<AiSysUser> getAllUsers() {
        return aiSysUserRepository.findAll();
    }
    
}
