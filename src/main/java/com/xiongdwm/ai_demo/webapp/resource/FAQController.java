package com.xiongdwm.ai_demo.webapp.resource;

import com.xiongdwm.ai_demo.webapp.entities.FAQ;
import com.xiongdwm.ai_demo.webapp.service.FAQService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FAQController {
    @Autowired
    private FAQService faqService;

    @PostMapping("/faq/page")
    public Page<FAQ> getFAQPage(@RequestParam("pageNo") int pageNo, @RequestParam("pageSize") int pageSize,@RequestParam("kid")Long knowledgeBaseId) {
        return faqService.list(pageNo, pageSize,knowledgeBaseId);
    }
}
