package com.xiongdwm.ai_demo.embedding;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.ai_demo.utils.config.Neo4jVectorStoreFactory;
import com.xiongdwm.ai_demo.utils.global.ApiResponse;
import com.xiongdwm.ai_demo.utils.global.WordSplitHelper;
import com.xiongdwm.ai_demo.webapp.entities.FileLog;
import com.xiongdwm.ai_demo.webapp.entities.KnowledgeBase;
import com.xiongdwm.ai_demo.webapp.service.FileLogService;

import reactor.core.publisher.Mono;

@RestController
public class EmbeddingApi {
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private Neo4jVectorStoreFactory vectorStoreFactory;
    @Autowired
    private FileLogService fileLogService;

    @Value("${file.upload.path}")
    private String uploadPath;

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
        VectorStore myVectorStore = vectorStoreFactory.createVectorStore("db_description", "db_description",
                embeddingModel);
        Document doc = new Document(text, Map.of("subdivision", "db_description"));
        myVectorStore.add(List.of(doc));
        return ApiResponse.success("Stored successfully!");
    }

    /**
     * @apiNote if embedding dimension is 768, need to mannually set the dimension
     *          of neo4j to 768 by Cypher:
     *          CALL
     *          db.index.vector.createNodeIndex('vec_index','default_doc_label',
     *          'embedding', 768, 'cosine')
     * @param input: question
     * @return documents
     */

    @PostMapping("/doc/search")
    public ApiResponse<List<Document>> searchDocument(@RequestParam("input") String input) {
        VectorStore myVectorStore = vectorStoreFactory.createVectorStore("db_description", "db_description",
                embeddingModel);
        List<Document> results = myVectorStore.similaritySearch(SearchRequest.builder()
                .query(input)
                .topK(20)
                .similarityThreshold(0.1)
                .build());
        return ApiResponse.success(results);

    }

    @PostMapping("/embedding/byDocPath")
    public ApiResponse<String> getEmbeddingByDocPath(@RequestParam("path") String path) {
        try {
            KnowledgeBase knowledgeBase = fileLogService.getByFilePath(path).getKnowledgeBase();
            if (knowledgeBase == null) {
                return ApiResponse.error("Knowledge base not found for the given file path.");
            }
            var tag= knowledgeBase.getTag();
            if(StringUtils.isBlank(tag.trim())) {
                return ApiResponse.error("Knowledge base tag is empty or whitespace only.");
            }
            List<String> list = WordSplitHelper.splitByParagraphs(path);
            VectorStore myVectorStore = vectorStoreFactory.createVectorStore(tag, tag,
                    embeddingModel);
            List<Document> documents = list.parallelStream()
                    .map(text -> new Document(text, Map.of("subdivision", "pro"))).collect(Collectors.toList());
            System.out.println("documents: "+documents.size());
            myVectorStore.add(documents);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ApiResponse.success("File processed successfully.");
    }

    @PostMapping(value = "/embedding/upload", consumes = "multipart/form-data", produces = "application/json")
    public Mono<ApiResponse<String>> upload(@RequestPart("file") FilePart filePart,@RequestParam("knowledgeBaseId")Long knowledgeBaseId) {
        String filePath = uploadPath + File.separator + filePart.filename();
        FileLog fileLog = new FileLog();
        fileLog.setId(0L);
        fileLog.setFileName(filePart.filename());
        fileLog.setFilePath(filePath);
        fileLog.setUploadTime(new Date());
        fileLog.setKnowledgeBaseId(knowledgeBaseId);
        fileLog.setFaculty(""); //之后从userToken里面取
        fileLogService.saveFileLog(fileLog);
        File dest = new File(filePath);
        return filePart.transferTo(dest)
                .then(Mono.fromCallable(() -> {
                    List<String> list = WordSplitHelper.splitByParagraphs(filePath);
                    list.stream().forEach(chunk->{
                        System.out.println();
                        System.out.println("chunk: "+chunk);
                    });
                    return ApiResponse.success(filePart.filename());
                }))
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(ApiResponse.error());
                });
    }

}