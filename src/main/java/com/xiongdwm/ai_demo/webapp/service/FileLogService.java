package com.xiongdwm.ai_demo.webapp.service;

import java.util.List;

import com.xiongdwm.ai_demo.webapp.entities.FileLog;
import com.xiongdwm.ai_demo.webapp.entities.KnowledgeBase;
import reactor.core.publisher.Flux;

public interface FileLogService {

    List<FileLog> showFileLog(Long knowledgeBaseId);

    void saveFileLog(FileLog fileLog);

    void saveKnowledgeBase(KnowledgeBase knowledgeBase);

    List<KnowledgeBase> showKnowledgeBases();

    Flux<List<KnowledgeBase>> knowledgeBaseFlux();

    void emitKnowledgeBaseUpdate();

    FileLog getByFilePath(String path); 
    
    KnowledgeBase getKnowledgeBaseByTag(String tag);

    boolean updateKnowledgeBaseInfo(KnowledgeBase knowledgeBase);


}
