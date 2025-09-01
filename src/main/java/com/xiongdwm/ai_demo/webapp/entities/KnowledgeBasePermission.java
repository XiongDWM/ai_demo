package com.xiongdwm.ai_demo.webapp.entities;

import java.sql.Date;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;


// @Table(name = "knowledgebase_permission")
// @Entity
public class KnowledgeBasePermission {
    @Id
    private Long id;
    @Column
    private String reason; //申请原因
    @Column(name = "user_id")
    private Long userId; //申请人ID
    @Column(name = "knowledgebase_id")
    private Long knowledgeBaseId; //知识库ID
    @Column(name = "allowed_duration")
    private Long allowedDuration; //允许访问时长，单位为小时
    @Column(name = "approved")
    private boolean approved=false; //是否已批准
    @Column(name ="active")
    private boolean active=false; //是否激活，激活后开始算时间
    @Column(name = "active_date")
    private Date activeDate; //激活时间
    @ManyToOne
    @JsonManagedReference
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private AiSysUser user;

    @ManyToOne
    @JsonManagedReference
    @JoinColumn(name = "knowledgebase_id", insertable = false, updatable = false)
    private KnowledgeBase knowledgeBase;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public Long getAllowedDuration() {
        return allowedDuration;
    }

    public void setAllowedDuration(Long allowedDuration) {
        this.allowedDuration = allowedDuration;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public AiSysUser getUser() {
        return user;
    }

    public void setUser(AiSysUser user) {
        this.user = user;
    }

    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    public void setKnowledgeBase(KnowledgeBase knowledgeBase) {
        this.knowledgeBase = knowledgeBase;
    }

    

}
