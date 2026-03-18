package com.xiongdwm.ai_demo.webapp.entities;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Excel 上传记录，关联到 EvaluateSnapshot。
 * tableName 记录动态创建的 MySQL 临时表名，用于后续清理。
 */
@Entity
@Table(name = "evaluate_excel_log")
public class EvaluateExcelLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name") // 文件名
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "table_name") //不显示
    private String tableName;

    @Column(name = "db_description", columnDefinition = "TEXT") //不显示
    private String dbDescription;

    @Column(name = "snapshot_id") //不显示
    private Long snapshotId;

    @Column(name = "upload_date") //上传时间
    private Date uploadDate;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }

    public String getDbDescription() { return dbDescription; }
    public void setDbDescription(String dbDescription) { this.dbDescription = dbDescription; }

    public Long getSnapshotId() { return snapshotId; }
    public void setSnapshotId(Long snapshotId) { this.snapshotId = snapshotId; }

    public Date getUploadDate() { return uploadDate; }
    public void setUploadDate(Date uploadDate) { this.uploadDate = uploadDate; }
}
