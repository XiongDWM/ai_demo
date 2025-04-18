package com.xiongdwm.ai_demo.embedding;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

@RestController
public class EmbeddingApi {
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private VectorStore vectorStore;

    @GetMapping("/doc/embedding")
    public EmbeddingResponse getEmbedding(@RequestParam("text") String text) {
        EmbeddingResponse embeddingResponse = this.embeddingModel.embedForResponse(List.of(text));
        System.out.println("demension" + embeddingResponse.getResults().get(0).getOutput().length);
        return embeddingResponse;
    }

    @PostMapping("/doc/store")
    public String storeEmbedding(@RequestParam("text") String text) {
        Document doc = new Document(text);
        vectorStore.add(List.of(doc));
        return "Stored successfully!";
    }

    @PostMapping("/doc/search")
    public List<Document> searchDocument(@RequestParam("input")String input){
        EmbeddingResponse embeddingResponse = this.embeddingModel.embedForResponse(List.of(input));
        System.out.println("demension: " + embeddingResponse.getResults().get(0).getOutput().length);
        System.out.println("====================================================================");
        // float[] embedding = embeddingResponse.getResults().get(0).getOutput();

        List<Document> results = vectorStore.similaritySearch(SearchRequest.builder()
        .query(input)
        .topK(1) 
        .build());
        return results;

    }

}