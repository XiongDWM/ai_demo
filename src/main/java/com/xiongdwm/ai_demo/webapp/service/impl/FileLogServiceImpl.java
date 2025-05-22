package com.xiongdwm.ai_demo.webapp.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.xiongdwm.ai_demo.webapp.entities.FileLog;
import com.xiongdwm.ai_demo.webapp.repository.FileLogRepo;
import com.xiongdwm.ai_demo.webapp.service.FileLogService;

import jakarta.annotation.Resource;

@Service
public class FileLogServiceImpl implements FileLogService{
    
    @Resource
    private FileLogRepo fileLogRepo;

    @Override
    public List<FileLog> showFileLog() {
        return fileLogRepo.findAll();
    }

    @Override
    public void saveFileLog(FileLog fileLog) {
        fileLogRepo.save(fileLog);
    }



}
