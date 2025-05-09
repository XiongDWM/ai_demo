package com.xiongdwm.ai_demo.utils.config;


import org.neo4j.driver.Driver;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore.Neo4jDistanceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;



@Component
public class Neo4jVectorStoreFactory {
    
    @Autowired
    private Driver driver;


    public VectorStore createVectorStore(String label,String index,@Lazy EmbeddingModel embeddingModel) {
        return Neo4jVectorStore.builder(driver,embeddingModel)
        .databaseName("neo4j")
        .distanceType(Neo4jDistanceType.COSINE) 
        .embeddingDimension(768) 
        .embeddingProperty("embedding")       
        .indexName(index)
        .label(label)
        .initializeSchema(true)
        .build();
            
    }





}
