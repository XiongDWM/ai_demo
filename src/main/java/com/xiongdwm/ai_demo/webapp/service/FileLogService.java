package com.xiongdwm.ai_demo.webapp.service;

import java.util.List;

import com.xiongdwm.ai_demo.webapp.entities.FileLog;

public interface FileLogService {

    List<FileLog> showFileLog();

    void saveFileLog(FileLog fileLog);

    
    
}
