package com.xiongdwm.ai_demo.webapp.entities;

import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * 一次 Excel 评估任务快照。
 * 每个 Snapshot 对应一个独立的向量库（tag = "eval_" + id），
 * 存储所有关联 Excel 临时表的字段描述 + 跨表关系描述。
 */
@Entity
@Table(name = "evaluate_snapshot")
public class EvaluateSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户定义的跨表关系描述，如 "表1的A列对应表2的C列" */
    @Column(columnDefinition = "TEXT")
    private String tableRelation;

    /** 向量库标识，格式 eval_{id}，自动生成 */
    @Column(name = "vector_tag")
    private String vectorTag;

    /** 最近一次用户问题 */
    @Column(columnDefinition = "TEXT")
    private String lastQuestion;

    /** 最近一次生成的 SQL */
    @Column(columnDefinition = "TEXT")
    private String lastSql;

    @Column
    private Date createDate;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "snapshot_id")
    private List<EvaluateExcelLog> evaluateExcelLogs;

    public EvaluateSnapshot() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTableRelation() { return tableRelation; }
    public void setTableRelation(String tableRelation) { this.tableRelation = tableRelation; }

    public String getVectorTag() { return vectorTag; }
    public void setVectorTag(String vectorTag) { this.vectorTag = vectorTag; }

    public String getLastQuestion() { return lastQuestion; }
    public void setLastQuestion(String lastQuestion) { this.lastQuestion = lastQuestion; }

    public String getLastSql() { return lastSql; }
    public void setLastSql(String lastSql) { this.lastSql = lastSql; }

    public Date getCreateDate() { return createDate; }
    public void setCreateDate(Date createDate) { this.createDate = createDate; }

    public List<EvaluateExcelLog> getEvaluateExcelLogs() { return evaluateExcelLogs; }
    public void setEvaluateExcelLogs(List<EvaluateExcelLog> evaluateExcelLogs) { this.evaluateExcelLogs = evaluateExcelLogs; }

    /**
     * 生成向量库 tag：eval_{id}
     */
    public String generateVectorTag() {
        this.vectorTag = "eval_" + this.id;
        return this.vectorTag;
    }
}
