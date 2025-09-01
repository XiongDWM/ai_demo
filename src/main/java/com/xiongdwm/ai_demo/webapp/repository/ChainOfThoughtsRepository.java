package com.xiongdwm.ai_demo.webapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.xiongdwm.ai_demo.webapp.entities.ChainOfThoughts;


@Repository
public interface ChainOfThoughtsRepository extends JpaRepository<ChainOfThoughts, Long>, JpaSpecificationExecutor<ChainOfThoughts> {
    
    
}
