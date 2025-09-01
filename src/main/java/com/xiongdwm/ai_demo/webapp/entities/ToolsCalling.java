package com.xiongdwm.ai_demo.webapp.entities;


import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tools_calling")
public class ToolsCalling {
    @Id
    public String id;
    @Column(name = "cot_id", nullable = false)
    public String cotId;
    @Column
    public String toolName;
    @Column
    public String toolArgs;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cot_id", referencedColumnName = "id", nullable = true,insertable = false, updatable = false)
    @JsonManagedReference
    public ChainOfThoughts chainOfThoughts;
    
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getCotId() {
        return cotId;
    }
    public void setCotId(String cotId) {
        this.cotId = cotId;
    }
    public String getToolName() {
        return toolName;
    }
    public void setToolName(String toolName) {
        this.toolName = toolName;
    }
    public String getToolArgs() {
        return toolArgs;
    }
    public void setToolArgs(String toolArgs) {
        this.toolArgs = toolArgs;
    }
    public ChainOfThoughts getChainOfThoughts() {
        return chainOfThoughts;
    }
    public void setChainOfThoughts(ChainOfThoughts chainOfThoughts) {
        this.chainOfThoughts = chainOfThoughts;
    }

    
}
