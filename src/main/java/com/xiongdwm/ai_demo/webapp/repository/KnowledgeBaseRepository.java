package com.xiongdwm.ai_demo.webapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.xiongdwm.ai_demo.webapp.entities.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase,Long>, JpaSpecificationExecutor<KnowledgeBase> {
    Optional<KnowledgeBase> findOneByName(String name);
    Optional<KnowledgeBase> findOneByTag(String tag);
}
