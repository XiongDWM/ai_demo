package com.xiongdwm.ai_demo.webapp.entities;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Table(name = "knowledge_base")
@Entity
public class KnowledgeBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column (name = "name", nullable = false, unique = true)
    private String name; //知识库名称
    @Column (name = "description")
    private String description; //知识库描述
    @Column (name = "authorized_character") 
    private String authorizedCharacter; //允许访问的账号id
    @Column (name = "tag", nullable = true, unique = true)
    private String tag; //标签 
    @OneToMany(fetch = FetchType.EAGER,mappedBy = "knowledgeBase")
    @JsonBackReference
    private List<FileLog> fileLogs; //文件日志列表
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "knowledgebase_user",
        joinColumns = @JoinColumn(name = "knowledgebase_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonManagedReference
    private List<AiSysUser> userPermissions;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getAuthorizedCharacter() {
        return authorizedCharacter;
    }
    public void setAuthorizedCharacter(String authorizedCharacter) {
        this.authorizedCharacter = authorizedCharacter;
    }
    public String getTag() {
        return tag;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }
    public List<FileLog> getFileLogs() {
        return fileLogs;
    }
    public void setFileLogs(List<FileLog> fileLogs) {
        this.fileLogs = fileLogs;
    }
    public List<AiSysUser> getUserPermissions() {
        return userPermissions;
    }
    public void setUserPermissions(List<AiSysUser> userPermissions) {
        this.userPermissions = userPermissions;
    }
    @Override
    public String toString() {
        return "KnowledgeBase [id=" + id + ", name=" + name + ", description=" + description + ", authorizedCharacter="
                + authorizedCharacter + ", tag=" + tag + ", fileLogs=" + fileLogs + ", userPermissions="
                + userPermissions + "]";
    } 

    
           
}
