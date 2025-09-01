package com.xiongdwm.ai_demo.webapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.xiongdwm.ai_demo.webapp.entities.ToolsCalling;

@Repository
public interface ToolsCallingRepository extends JpaRepository<ToolsCalling, String>, JpaSpecificationExecutor<ToolsCalling> {

}
