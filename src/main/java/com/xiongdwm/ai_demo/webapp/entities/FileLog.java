package com.xiongdwm.ai_demo.webapp.entities;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;

@Entity
@Table(name = "file_log")
public class FileLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="file_name",unique = true,nullable = false)
    private String fileName;
    @Column(name="file_path",unique = true,nullable = false)
    private String filePath;
    @Column
    private Date uploadTime;
    @Column
    @Enumerated(EnumType.STRING)
    private ProcessingState processingState = ProcessingState.PENDING;
    @Column
    private String faculty;
    @Column(name = "knowledge_base_id", nullable = true)
    private Long knowledgeBaseId; 

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "knowledge_base_id", referencedColumnName = "id", nullable = true,insertable = false, updatable = false)
    @JsonManagedReference
    private KnowledgeBase knowledgeBase;

    public enum ProcessingState {
        PENDING("未解析"),
        PROCESSING("解析中"),
        COMPLETED("解析完成"),
        FAILED("解析失败");

        private final String label;

        ProcessingState(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    public FileLog() {
    }

    public FileLog(Long id, String fileName, String filePath, Date uploadTime, String faculty) {
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

    public ProcessingState getProcessingState() {
        return processingState;
    }

    public void setProcessingState(ProcessingState processingState) {
        this.processingState = processingState;
    }
    
    public Long getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    public void setKnowledgeBase(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }
    public String getProcessingStateLabel(){
        return processingState != null ? processingState.getLabel() : "";
    }

    @Override
    public String toString() {
        return "FileLog{" +
                "fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                ", uploadTime=" + uploadTime +
                ", faculty='" + faculty + '\'' +
                ", processingState=" + getProcessingStateLabel() +
                '}';
    }


}
