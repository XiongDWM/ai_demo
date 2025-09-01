package com.xiongdwm.ai_demo.webapp.repository;

import java.util.Optional;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.xiongdwm.ai_demo.webapp.entities.AiSysUser;

@Repository
public interface AiSysUserRepository extends JpaRepository<AiSysUser, Long>, JpaSpecificationExecutor<AiSysUser> {
    // Define any additional query methods if needed
    // For example:
    // Optional<AiSysUser> findByUsername(String username);
    Optional<AiSysUser> findByUsername(String username);
    Optional<AiSysUser> findById(Long id);
    
}
