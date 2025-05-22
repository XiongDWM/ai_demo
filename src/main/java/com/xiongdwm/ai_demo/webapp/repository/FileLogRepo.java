package com.xiongdwm.ai_demo.webapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.xiongdwm.ai_demo.webapp.entities.FileLog;

@Repository
public interface FileLogRepo extends JpaRepository<FileLog, Long> {
    
    
}
