package com.xiongdwm.ai_demo.webapp.entities;

import com.xiongdwm.ai_demo.embedding.annotaion.AiVectorize;

import jakarta.persistence.Table;

@Table(name = "test_entity")
@AiVectorize(name = "test",description = "entity for software testing",type = AiVectorize.AiVectorizeType.ENTITY)
public class TestEntity {
    @AiVectorize(name = "id",description = "primary key for test",type = AiVectorize.AiVectorizeType.FIELDS)
    private Long id;
    @AiVectorize(name = "name",description = "name for test",type = AiVectorize.AiVectorizeType.FIELDS)
    private String name;
    
    

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
}
