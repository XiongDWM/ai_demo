package com.xiongdwm.ai_demo.webapp.entities;

import com.xiongdwm.ai_demo.embedding.annotaion.AiVectorize;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "test_entity")
@AiVectorize(name = "entity for software testing(test_entity)",description = "test record store in this table, properties are as follows:",type = AiVectorize.AiVectorizeType.ENTITY)
public class TestEntity {
    @AiVectorize(name = "id",description = "primary key for test, auto increment",type = AiVectorize.AiVectorizeType.FIELDS)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @AiVectorize(name = "name",description = "name for test, used for identification",type = AiVectorize.AiVectorizeType.FIELDS)
    @Column(name = "name")
    private String name; 
    @AiVectorize(name = "type",description = "type for test, used for classify, there has 3 types of test: test, production and unknown",type = AiVectorize.AiVectorizeType.FIELDS)
    @Column(name = "type")
    private TestEntityType type;

    public enum TestEntityType {
        TEST("test"),
        PRODUCTION("production"),
        UNKNOWN("unknown");

        private final String label;
        TestEntityType(String label) {
            this.label = label;
        }
        public String getLabel() {
            return label;
        }
    }
    

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
    public TestEntityType getType() {
        return type;
    }
    public void setType(TestEntityType type) {
        this.type = type;
    }
}
