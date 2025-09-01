package com.xiongdwm.ai_demo.webapp.entities;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "chain_of_thoughts")
public class ChainOfThoughts {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column
    public String question;
    @Column
    public String answer;
    @OneToMany(mappedBy = "chainOfThoughts")
    @JsonBackReference
    public List<ToolsCalling> toolsCallings; // 工具调用列表 

    
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getQuestion() {
        return question;
    }
    public void setQuestion(String question) {
        this.question = question;
    }
    public String getAnswer() {
        return answer;
    }
    public void setAnswer(String answer) {
        this.answer = answer;
    }
    public List<ToolsCalling> getToolsCallings() {
        return toolsCallings;
    }
    public void setToolsCallings(List<ToolsCalling> toolsCallings) {
        this.toolsCallings = toolsCallings;
    }
    
    
}
