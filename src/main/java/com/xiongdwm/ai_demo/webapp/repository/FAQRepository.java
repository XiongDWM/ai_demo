package com.xiongdwm.ai_demo.webapp.repository;

import com.xiongdwm.ai_demo.webapp.entities.FAQ;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface FAQRepository extends JpaRepository<FAQ,Long>, JpaSpecificationExecutor<FAQ> {
    void deleteByVectorNodeId(String vectorNodeId);
    Page<FAQ> findAllByKnowledgeBaseId(Long knowledgeBaseId, Pageable pageable);
    FAQ findOneByVectorNodeId(String vectorNodeId);
}
