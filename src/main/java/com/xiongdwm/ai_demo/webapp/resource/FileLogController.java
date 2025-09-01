package com.xiongdwm.ai_demo.webapp.resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;


import com.xiongdwm.ai_demo.utils.config.Neo4jVectorStoreFactory;
import com.xiongdwm.ai_demo.utils.global.ApiResponse;
import com.xiongdwm.ai_demo.webapp.entities.FileLog;
import com.xiongdwm.ai_demo.webapp.entities.KnowledgeBase;
import com.xiongdwm.ai_demo.webapp.service.FileLogService;

import reactor.core.publisher.Mono;

@RestController
public class FileLogController {
    @Autowired
    private FileLogService fileLogService;
    @Autowired
    Neo4jVectorStoreFactory vectorStore;
    @Value("${file.upload.path}")
    private String uploadPath;

    @PostMapping("/fileLog/show")
    public List<FileLog> showFileLog(@RequestParam(name = "knowledgeBaseId", required = false) Long knowledgeBaseId) {
        System.out.println(knowledgeBaseId);
        return fileLogService.showFileLog(knowledgeBaseId);
    }

    @PostMapping("/knowledgeBase/save")
    public ApiResponse<String> saveKnowledgeBase(KnowledgeBase knowledgeBase) {
        System.out.println(knowledgeBase.toString());
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

    @PostMapping("/fileLog/getFileByPath")
    public ResponseEntity<Resource> getFileByPath(@RequestParam String filePath) {
        FileLog fileLog = fileLogService.getByFilePath(filePath);
        if (fileLog == null) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(fileLog.getFilePath());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileLog.getFileName() + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }

    @PostMapping(value = "/file/upload", consumes = "multipart/form-data", produces = "application/json")
    public Mono<ApiResponse<String>> uploadFile(@RequestPart("file") FilePart file)throws IllegalStateException, IOException {
        String filePath = uploadPath + File.separator + file.filename();
        File dest = new File(filePath);
        return file.transferTo(dest)
                .then(Mono.fromCallable(() -> {
                    return ApiResponse.success(filePath);
                }))
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(ApiResponse.error());
                });
    }

}
