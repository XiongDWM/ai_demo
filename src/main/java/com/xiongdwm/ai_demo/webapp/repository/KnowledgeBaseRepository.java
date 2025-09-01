package com.xiongdwm.ai_demo.webapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xiongdwm.ai_demo.webapp.entities.KnowledgeBase;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase,Long>{
    Optional<KnowledgeBase> findOneByName(String name);
    Optional<KnowledgeBase> findOneByTag(String tag);
}
