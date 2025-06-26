package com.xiongdwm.ai_demo.webapp.service.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import com.xiongdwm.ai_demo.webapp.entities.FileLog;
import com.xiongdwm.ai_demo.webapp.entities.KnowledgeBase;
import com.xiongdwm.ai_demo.webapp.repository.FileLogRepo;
import com.xiongdwm.ai_demo.webapp.repository.KnowledgeBaseRepository;
import com.xiongdwm.ai_demo.webapp.service.FileLogService;

import jakarta.annotation.Resource;

@Service
public class FileLogServiceImpl implements FileLogService{
    
    @Resource
    private FileLogRepo fileLogRepo;
    @Resource
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Override
    public List<FileLog> showFileLog(Long knowledgeBaseId) {
        if(knowledgeBaseId!=null&&knowledgeBaseId>0L){
            KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId).orElse(null);
            if(knowledgeBase ==null)return Collections.emptyList();

            return knowledgeBase.getFileLogs(); // Return an empty list if the knowledge base is not found
        }
        return fileLogRepo.findAll();
    }

    @Override
    public void saveFileLog(FileLog fileLog) {
        fileLogRepo.save(fileLog);
    }

    @Override
    public void saveKnowledgeBase(KnowledgeBase knowledgeBase) {
        knowledgeBaseRepository.save(knowledgeBase);
    }

    @Override
    public List<KnowledgeBase> showKnowledgeBases() {
        return knowledgeBaseRepository.findAll();
    }

    @Override
    public FileLog getByFilePath(String path) {
        // TODO Auto-generated method stub
        return fileLogRepo.findOneByFilePath(path).orElse(null);
    }



    

}
