package com.xiongdwm.ai_demo.webapp.resource;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.ai_demo.webapp.entities.FileLog;
import com.xiongdwm.ai_demo.webapp.service.FileLogService;

@RestController
public class FileLogController {
    @Autowired
    private FileLogService fileLogService;

    @PostMapping("/fileLog/show")
    public List<FileLog> showFileLog() {
        return fileLogService.showFileLog();
    }
}
