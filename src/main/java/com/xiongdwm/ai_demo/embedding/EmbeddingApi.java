package com.xiongdwm.ai_demo.embedding;


import java.util.List;
import java.util.Map;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.ai_demo.utils.config.Neo4jVectorStoreFactory;
import com.xiongdwm.ai_demo.utils.global.ApiResponse;


@RestController
public class EmbeddingApi {
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private VectorStore vectorStore;
    @Autowired
    private Neo4jVectorStoreFactory vectorStoreFactory;

    @GetMapping("/doc/embedding")
    public EmbeddingResponse getEmbedding(@RequestParam("text") String text) {
        EmbeddingResponse embeddingResponse = this.embeddingModel.embedForResponse(List.of(text));
        System.out.println("demension: " + embeddingResponse.getResults().get(0).getOutput().length);
        System.out.println("====================================================================");
        System.out.println(embeddingResponse.getResults().get(0).getOutput());

        System.out.println("Embedding Response: " + embeddingResponse);
        
        return embeddingResponse;
    }

    @PostMapping("/doc/store")
    public ApiResponse<String> storeEmbedding(@RequestParam("text") String text) {
        VectorStore myVectorStore = vectorStoreFactory.createVectorStore("base_knowledge", "base_knowledge", embeddingModel);
        Document doc = new Document(text, Map.of("subdivision", "base"));
        myVectorStore.add(List.of(doc));
        return ApiResponse.success("Stored successfully!");
    }

    /**
     * @apiNote if embedding dimension is 768, need to mannually set the dimension of neo4j to 768 by Cypher:
     *          CALL db.index.vector.createNodeIndex('vec_index','default_doc_label', 'embedding', 768, 'cosine') 
     * @param input: question
     * @return documents
     */

    @PostMapping("/doc/search")
    public ApiResponse<List<Document>> searchDocument(@RequestParam("input") String input){
        VectorStore myVectorStore = vectorStoreFactory.createVectorStore("base_knowledge", "base_knowledge", embeddingModel);
        List<Document> results = myVectorStore.similaritySearch(SearchRequest.builder()
        .query(input)
        .topK(5)
        .build());
        return ApiResponse.success(results);

    }

}