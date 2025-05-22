package com.xiongdwm.ai_demo.webapp.entities;


import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "file_log")
public class FileLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String fileName;
    @Column
    private String filePath;
    @Column
    private Date uploadTime;
    @Column
    private String faculty;

    public FileLog() {
    }

    public FileLog(Long id,String fileName, String filePath, Date uploadTime, String faculty) {
        this.id = id;
        this.fileName = fileName;
        this.filePath = filePath;
        this.uploadTime = uploadTime;
        this.faculty = faculty;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getFileName() {
        return fileName;
    }
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    public String getFilePath() {
        return filePath;
    }
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    public Date getUploadTime() {
        return uploadTime;
    }
    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }
    public String getFaculty() {
        return faculty;
    }
    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }
    @Override
    public String toString() {
        return "FileLog{" +
                "fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", uploadTime=" + uploadTime +
                ", faculty='" + faculty + '\'' +
                '}';
    }
}
