package com.xiongdwm.ai_demo.webapp.service;

import com.xiongdwm.ai_demo.webapp.entities.FAQ;
import org.springframework.data.domain.Page;

public interface FAQService {
    FAQ add(FAQ entity);
    void deleteByVectorNodeId(String id);
    Page<FAQ> list(int pageNo, int pageSize,Long knowledgeBaseId);
    FAQ findOneByVectorNodeId(String vectorNodeId);
}
