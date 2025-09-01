package com.xiongdwm.ai_demo.tools;

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

@Component
public class EmbeddingTool {
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private Neo4jVectorStoreFactory vectorStoreFactory;

    @Tool(name = "baseKnowledgeRetrieve", description = "根据输入的消息查询相关知识库内容")
    public List<Document> baseKnowledgeRetrieve(@ToolParam(description = "用户输入的问题")String message) {
        VectorStore myVectorStore = vectorStoreFactory.createVectorStore("base_knowledge", "base_knowledge",
                embeddingModel);
        List<Document> results = myVectorStore.similaritySearch(SearchRequest.builder()
                .query(message)
                .topK(18)
                .similarityThreshold(0.8f)
                .build());
        System.out.println("description results: " + results.size());
        return results;
    }
    
}
