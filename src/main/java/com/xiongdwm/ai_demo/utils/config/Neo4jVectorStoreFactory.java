package com.xiongdwm.ai_demo.utils.config;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.ai.embedding.EmbeddingModel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore;
import org.springframework.ai.vectorstore.neo4j.Neo4jVectorStore.Neo4jDistanceType;

@Component
public class Neo4jVectorStoreFactory {

    @Autowired
    private Driver driver;

    public VectorStore createVectorStore(String label, String index, @Lazy EmbeddingModel embeddingModel) {
        return Neo4jVectorStore.builder(driver, embeddingModel)
                .databaseName("neo4j")
                .distanceType(Neo4jDistanceType.COSINE)
                .embeddingDimension(768)
                .embeddingProperty("embedding")
                .indexName(index)
                .label(label)
                .initializeSchema(true)
                .build();
    }

    public void createVectorIndex(String index, String label, int dimension, String property, String similarity) {
        String cypher = String.format(
                "CALL db.index.vector.createNodeIndex('%s','%s','%s', %d, '%s')",
                index, label, property, dimension, similarity);
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                tx.run(cypher);
                return null;
            });
        } catch (Exception e) {
            System.out.println("Neo4j 索引创建异常（可能已存在）: " + e.getMessage());
        }
    }

}
