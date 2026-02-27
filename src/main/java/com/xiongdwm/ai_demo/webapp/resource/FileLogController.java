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
import org.springframework.web.bind.annotation.*;


import com.xiongdwm.ai_demo.utils.config.Neo4jVectorStoreFactory;
import com.xiongdwm.ai_demo.utils.global.ApiResponse;
import com.xiongdwm.ai_demo.webapp.entities.FileLog;
import com.xiongdwm.ai_demo.webapp.entities.KnowledgeBase;
import com.xiongdwm.ai_demo.webapp.service.FileLogService;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class FileLogController {
    @Autowired
    private FileLogService fileLogService;
    @Autowired
    private Neo4jVectorStoreFactory vectorStore;
    @Value("${file.upload.path}")
    private String uploadPath;

    @PostMapping("/fileLog/show")
    public List<FileLog> showFileLog(@RequestParam(name = "knowledgeBaseId", required = false) Long knowledgeBaseId) {
        System.out.println(knowledgeBaseId);
        return fileLogService.showFileLog(knowledgeBaseId);
    }

    @PostMapping("/knowledgeBase/update")
    public ApiResponse<String> updateKnowledgeBase(KnowledgeBase knowledgeBase) {
        boolean success = fileLogService.updateKnowledgeBaseInfo(knowledgeBase);
        if (success) {
            fileLogService.emitKnowledgeBaseUpdate();
            return ApiResponse.success("知识库信息更新成功");
        } else {
            return ApiResponse.error("知识库信息更新失败");
        }
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
        fileLogService.emitKnowledgeBaseUpdate();
        return ApiResponse.success("知识库创建成功");
    }

    @CrossOrigin(originPatterns = "*")
    @GetMapping(value = "/knowledgeBase/flux/show",produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<List<KnowledgeBase>> showKnowledgeBasesStream(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestParam(value = "token", required = false) String tokenParam
    ) {
        return fileLogService.knowledgeBaseFlux();
    }

    @PostMapping(value = "/knowledgeBase/show",produces = "application/json")
    public List<KnowledgeBase>showKnowledgeBases(@RequestHeader(value = "Authorization", required = false) String token){
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
                    var path=dest.getPath();
                    if(path.contains("\\"))path=path.replace("\\", "/");
                    System.out.println(path);
                    return ApiResponse.success(path);
                }))
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(ApiResponse.error());
                });
    }

}
