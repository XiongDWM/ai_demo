package com.xiongdwm.ai_demo.embedding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.xiongdwm.ai_demo.embedding.ingest.EmbeddingService;
import com.xiongdwm.ai_demo.utils.global.ExcelParser;
import com.xiongdwm.ai_demo.webapp.entities.FAQ;
import com.xiongdwm.ai_demo.webapp.service.FAQService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
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
import reactor.core.scheduler.Schedulers;

@RestController
public class EmbeddingApi {
    @Autowired
    @Qualifier("ollamaEmbedding")
    private EmbeddingModel embeddingModel;
    @Autowired
    private Neo4jVectorStoreFactory vectorStoreFactory;
    @Autowired
    private FileLogService fileLogService;
    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private FAQService faqService;


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
        var vectorStore=embeddingService.vectorStore("db_description", "db_description");
        var results= embeddingService.searchDocuments(vectorStore,input,0.5,20);
        return ApiResponse.success(results);
    }

    @PostMapping("/embedding/byDocPath")
    //@RequestParam("path") String path,
    public Mono<ApiResponse<String>> getEmbeddingByDocPath(@RequestParam("logId")Long logId) {
        System.out.println(1);
        return Mono.fromCallable(()->{
            FileLog fileLog = fileLogService.getById(logId);
            if(fileLog == null) return ApiResponse.error("File not found for the given path.");
            try {
                KnowledgeBase knowledgeBase = fileLog.getKnowledgeBase();
                if (knowledgeBase == null) {
                    return ApiResponse.error("Knowledge base not found for the given file path.");
                }
                var tag= knowledgeBase.getTag();
                if(StringUtils.isBlank(tag.trim())) {
                    return ApiResponse.error("Knowledge base tag is empty or whitespace only.");
                }
                List<WordSplitHelper.Chunk> list = WordSplitHelper.splitChunkByWeight(fileLog.getFilePath());
                VectorStore myVectorStore = vectorStoreFactory.createVectorStore(tag, tag,
                        embeddingModel);
                var sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                List<Document> documents = list.parallelStream()
                        .map(WordSplitHelper.Chunk::toDocument).collect(Collectors.toList());
                myVectorStore.add(documents);
                fileLog.setProcessingState(FileLog.ProcessingState.COMPLETED);
                fileLogService.saveFileLog(fileLog);
            } catch (Exception e) {
                System.out.println(e.getLocalizedMessage());
                fileLog.setProcessingState(FileLog.ProcessingState.FAILED);
                fileLogService.saveFileLog(fileLog);
                return ApiResponse.error("Error processing file: " + e.getLocalizedMessage());
            }
            return ApiResponse.success("File processed successfully.");
        }).subscribeOn(Schedulers.boundedElastic());


    }

    @PostMapping("/qa/upload")
    public Mono<ApiResponse<String>>uploadQaPairs(@RequestParam("tag")String tag){
        var filePath="C:\\Users\\Admin\\Desktop\\zl\\faq.xlsx";
        var tagFAQ=tag+"_faq";
        embeddingService.createIndex(tagFAQ, tagFAQ, 768, "embedding", "cosine");
        var content= ExcelParser.importFile(filePath);

        return Mono.fromCallable(()->{
            VectorStore vs = embeddingService.vectorStore(tagFAQ, tagFAQ);
            var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            var date = sdf.format(new Date());
            List<Document>documents=new ArrayList<>(content.size());
            content.forEach(row->{
                String question=row[0];
                String answer=row[1];
                String text = "问：" + question + "\n答：" + answer;
                Map<String, Object> md = new HashMap<>();
                md.put("type", "faq");
                md.put("date", date);
                var doc = new Document(text, md);
                documents.add(doc);

                KnowledgeBase kb = fileLogService.getKnowledgeBaseByTag(tag);
                FAQ faq=new FAQ();
                faq.setVectorNodeId(doc.getId());
                faq.setDate(new Date());
                faq.setQuestion(question);
                faq.setAnswer(answer);
                faq.setKnowledgeBaseId(kb.getId());
                faqService.add(faq);
            });
            vs.add(documents);
            return ApiResponse.success("已导入FAQ问答");
        }).subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e->Mono.just(ApiResponse.error(e.getLocalizedMessage())));
    }

    @PostMapping("/qa/add")
    public Mono<ApiResponse<String>> addQaPair(@RequestParam("tag")String tag,@RequestParam("question")String question,@RequestParam("answer")String answer){
        var tagFAQ=tag+"_faq";
        embeddingService.createIndex(tagFAQ, tagFAQ, 768, "embedding", "cosine");
        return Mono.fromCallable(()->{
            VectorStore vs = embeddingService.vectorStore(tagFAQ, tagFAQ);
            var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            var date = sdf.format(new Date());
            String text = "问：" + question + "\n答：" + answer;
            Map<String, Object> md = new HashMap<>();
            md.put("type", "faq");
            md.put("date", date);
            var doc = new Document(text, md);
            vs.add(List.of(doc));

            KnowledgeBase kb = fileLogService.getKnowledgeBaseByTag(tag);
            FAQ faq=new FAQ();
            faq.setVectorNodeId(doc.getId());
            faq.setDate(new Date());
            faq.setQuestion(question);
            faq.setAnswer(answer);
            faq.setKnowledgeBaseId(kb.getId());
            faqService.add(faq);
            return ApiResponse.success("已写入FAQ问答");
        }).subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e->Mono.just(ApiResponse.error(e.getLocalizedMessage())));
    }

    @PostMapping("/qa/delete")
    public ApiResponse<String>deleteQaPair(@RequestParam("vectorNodeId")String vnId,@RequestParam("tag")String tag){
        var tagFAQ=tag+"_faq";
        try {
            faqService.deleteByVectorNodeId(vnId);
            VectorStore vs = embeddingService.vectorStore(tagFAQ, tagFAQ);

            var idList=List.of(vnId);
            vs.delete(idList);
            return ApiResponse.success("已删除FAQ问答");
        } catch (Exception e) {
            return ApiResponse.error("删除失败：" + e.getMessage());
        }
    }

    @PostMapping(value = "/embedding/upload", consumes = "multipart/form-data", produces = "application/json")
    public Mono<ApiResponse<String>> upload(@RequestPart("file") FilePart filePart,@RequestParam("knowledgeBaseId")Long knowledgeBaseId,@RequestHeader("Authorization") String token) {
        var timemillis=System.currentTimeMillis();
        String filePath = uploadPath + File.separator+"-"+timemillis + filePart.filename();
        String subfix= filePart.filename().substring(filePart.filename().lastIndexOf(".")+1).toLowerCase();
        if(subfix.equals("doc"))return Mono.just(ApiResponse.error("请转存成docx后上传，doc版本太旧"));
        if(subfix.equals("xlsx") || subfix.equals("xls")||subfix.equals("cvs")||subfix.equals("et"))return Mono.just(ApiResponse.error("暂不支持表格"));

        FileLog fileLog = new FileLog();
        String username = token.split("-")[0];
//        fileLog.setId(0L);
        fileLog.setFileName(filePart.filename());
        fileLog.setFilePath(filePath);
        fileLog.setUploadTime(new Date());
        fileLog.setKnowledgeBaseId(knowledgeBaseId);
        fileLog.setFaculty(username); //之后从userToken里面取
        fileLogService.saveFileLog(fileLog);
        File dest = new File(filePath);
        return filePart.transferTo(dest)
                .then(Mono.fromCallable(() -> {
                    List<String> list = WordSplitHelper.splitByParagraphs(filePath);    
                    list.forEach(chunk->{
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