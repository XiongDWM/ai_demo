package com.xiongdwm.ai_demo.webapp.service.impl;

import com.xiongdwm.ai_demo.webapp.entities.FAQ;
import com.xiongdwm.ai_demo.webapp.repository.FAQRepository;
import com.xiongdwm.ai_demo.webapp.service.FAQService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class FAQServiceImpl implements FAQService {
    @Autowired
    private FAQRepository faqRepository;

    @Override
    public FAQ add(FAQ entity) {
        return faqRepository.save(entity);
    }

    @Override
    public void deleteByVectorNodeId(String id) {
        faqRepository.deleteByVectorNodeId(id);
    }

    @Override
    public Page<FAQ> list(int pageNo, int pageSize,Long knowledgeBaseId) {
        var pageable = PageRequest.of(pageNo - 1, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        return faqRepository.findAllByKnowledgeBaseId(knowledgeBaseId,pageable);
    }

    @Override
    public FAQ findOneByVectorNodeId(String vectorNodeId) {
        return faqRepository.findOneByVectorNodeId(vectorNodeId);
    }
}
