package com.xiongdwm.ai_demo.webapp.resource;

import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import com.xiongdwm.ai_demo.utils.config.Neo4jVectorStoreFactory;
import com.xiongdwm.ai_demo.utils.global.ApiResponse;
import com.xiongdwm.ai_demo.webapp.entities.FileLog;
import com.xiongdwm.ai_demo.webapp.entities.KnowledgeBase;
import com.xiongdwm.ai_demo.webapp.service.FileLogService;

@RestController
public class FileLogController {
    @Autowired
    private FileLogService fileLogService;
    @Autowired
    Neo4jVectorStoreFactory vectorStore;

    @PostMapping("/fileLog/show")
    public List<FileLog> showFileLog(@RequestParam(name = "knowledgeBaseId", required = false) Long knowledgeBaseId) {
        System.out.println(knowledgeBaseId);
        return fileLogService.showFileLog(knowledgeBaseId);
    }

    @PostMapping("/knowledgeBase/save")
    public ApiResponse<String> saveKnowledgeBase(KnowledgeBase knowledgeBase) {
        var tag = knowledgeBase.getTag();
        if (StringUtils.isEmpty(tag)|| tag.trim().length() < 2) {
            // todo uuid 随机
            UUID uuid = UUID.randomUUID();
            tag = uuid.toString();
            knowledgeBase.setTag(tag);
        } else {
            if (tag.trim().split("_").length <= 1)
                return ApiResponse.error("知识库标签格式出错，下划线不能作为头部或结尾");
        }
        System.out.println(tag);
        vectorStore.createVectorIndex(tag,tag,768,"embedding","cosine");
        fileLogService.saveKnowledgeBase(knowledgeBase);
        return ApiResponse.success("知识库创建成功");
    }

    @PostMapping("/knowledgeBase/show")
    public List<KnowledgeBase> showKnowledgeBases(@RequestHeader("Authorization") String token) {
        return fileLogService.showKnowledgeBases();
    }
}
