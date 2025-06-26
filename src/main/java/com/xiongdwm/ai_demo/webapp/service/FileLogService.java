package com.xiongdwm.ai_demo.webapp.service;

import java.util.List;

import com.xiongdwm.ai_demo.webapp.entities.FileLog;
import com.xiongdwm.ai_demo.webapp.entities.KnowledgeBase;

public interface FileLogService {

    List<FileLog> showFileLog(Long knowledgeBaseId);

    void saveFileLog(FileLog fileLog);

    void saveKnowledgeBase(KnowledgeBase knowledgeBase);

    List<KnowledgeBase> showKnowledgeBases();
    
    FileLog getByFilePath(String path); 
    
}
