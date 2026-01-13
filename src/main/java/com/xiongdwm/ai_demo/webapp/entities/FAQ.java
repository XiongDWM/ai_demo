package com.xiongdwm.ai_demo.webapp.entities;

import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name="faq")
public class FAQ {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String question;
    @Column(nullable = false)
    private String answer;
    @Column(unique = true,nullable  = false)
    private String vectorNodeId;  // one-to-one mapping to vector store node
    @Column
    private Date date;
    @Column(name="kid",nullable = false)
    private Long knowledgeBaseId;

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

    public String getVectorNodeId() {
        return vectorNodeId;
    }

    public void setVectorNodeId(String vectorNodeId) {
        this.vectorNodeId = vectorNodeId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Long getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(Long knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }
}
