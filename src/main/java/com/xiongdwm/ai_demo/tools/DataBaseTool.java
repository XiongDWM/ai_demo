package com.xiongdwm.ai_demo.tools;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.xiongdwm.ai_demo.utils.config.Neo4jVectorStoreFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class DataBaseTool {
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private Neo4jVectorStoreFactory vectorStoreFactory;
    @PersistenceContext
    private EntityManager entityManager;

    @Tool(name = "dbDescriptionRetrieve", description = "根据输入的消息查询数据库描述文档, 返回相关实体类的描述信息，用于理解实体类的字段和含义")
    public List<Document> dbDescriptionRetrieve(@ToolParam(description = "用户输入的问题")String message) {
        System.out.println("dbDescriptionRetrieve: " + message);
        VectorStore myVectorStore = vectorStoreFactory.createVectorStore("db_description", "db_description",
                embeddingModel);
        List<Document> results = myVectorStore.similaritySearch(SearchRequest.builder()
                .query(message)
                .topK(20)
                .similarityThreshold(0.6f)
                .build());
        if(null==results||results.isEmpty())return Collections.emptyList();
        System.out.println("description results: " + results.size());
        return results;
    }

    @Tool(name = "sqlGenerator", description = "根据输入的消息生成SQL语句")
    public String sqlGenerator(@ToolParam(description = "用户输入的问题") String message,
                                    @ToolParam(description = "实体类描述") List<Document> dbDescription) {
        // 这里可以实现SQL生成的逻辑
        // 例如，使用自然语言处理模型将message转换为SQL语句
        // 目前仅返回一个示例SQL语句
        return "SELECT * FROM example_table WHERE condition = 'example'";
    }
}
