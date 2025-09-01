package com.xiongdwm.ai_demo.webapp.entities;


import java.io.Serializable;
import java.util.List;


import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Table(name="ai_sys_user")
@Entity
public class AiSysUser implements Serializable{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(nullable = false)
    private String realName;
    @Column
    @JsonIgnore
    private String password;
    @ManyToMany(fetch = FetchType.EAGER, mappedBy = "userPermissions")
    @JsonBackReference
    private List<KnowledgeBase> knowledgeBases;

    public AiSysUser(){

    }
    public AiSysUser(Long id, String username, String password, List<KnowledgeBase> knowledgeBases) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.knowledgeBases = knowledgeBases;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public List<KnowledgeBase> getKnowledgeBases() {
        return knowledgeBases;
    }
    public void setKnowledgeBases(List<KnowledgeBase> knowledgeBases) {
        this.knowledgeBases = knowledgeBases;
    }
    public String getRealName() {
        return realName;
    }
    public void setRealName(String realName) {
        this.realName = realName;
    }

    

}
