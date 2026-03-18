package com.xiongdwm.ai_demo.embedding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.Date;

import com.xiongdwm.ai_demo.ingest.EmbeddingService;
import com.xiongdwm.ai_demo.webapp.entities.FAQ;
import com.xiongdwm.ai_demo.webapp.service.FAQService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.ai_demo.utils.config.Neo4jVectorStoreFactory;
import com.xiongdwm.ai_demo.utils.global.ApiResponse;
import com.xiongdwm.ai_demo.utils.global.ExcelParseHelper;
import com.xiongdwm.ai_demo.utils.global.WordSplitHelper;
import com.xiongdwm.ai_demo.webapp.entities.FileLog;
import com.xiongdwm.ai_demo.webapp.entities.KnowledgeBase;
import com.xiongdwm.ai_demo.webapp.service.FileLogService;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@RestController
public class EmbeddingApi {
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private Neo4jVectorStoreFactory vectorStoreFactory;
    @Autowired
    private FileLogService fileLogService;
    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private FAQService faqService;
    @Autowired
    private ExcelParseHelper excelParseHelper;


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
    public ApiResponse<String> getEmbeddingByDocPath(@RequestParam("path") String path) {
        FileLog fileLog = fileLogService.getByFilePath(path);
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
            List<String> list = WordSplitHelper.splitByParagraphs(path);
            VectorStore myVectorStore = vectorStoreFactory.createVectorStore(tag, tag,
                    embeddingModel);
            var sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            var date = sdf.format(new Date());
            List<Document> documents = list.parallelStream()
                    .map(text -> new Document(text, Map.of("date",date))).collect(Collectors.toList());
            System.out.println("documents: "+documents.size());
            myVectorStore.add(documents);
            fileLog.setProcessingState(FileLog.ProcessingState.COMPLETED);
            fileLogService.saveFileLog(fileLog);
        } catch (Exception e) {
            fileLog.setProcessingState(FileLog.ProcessingState.FAILED);
            fileLogService.saveFileLog(fileLog);
            return ApiResponse.error("Error processing file: " + e.getLocalizedMessage());
        }
        return ApiResponse.success("File processed successfully.");
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
            VectorStore vs = embeddingService.vectorStore(tagFAQ, tagFAQ);

            var idList=List.of(vnId);
            vs.delete(idList);
            faqService.deleteByVectorNodeId(vnId);
            return ApiResponse.success("已删除FAQ问答");
        } catch (Exception e) {
            return ApiResponse.error("删除失败：" + e.getMessage());
        }
    }

    @PostMapping(value = "/embedding/upload", consumes = "multipart/form-data", produces = "application/json")
    public Mono<ApiResponse<String>> upload(@RequestPart("file") FilePart filePart,@RequestParam("knowledgeBaseId")Long knowledgeBaseId,@RequestHeader("Authorization") String token) {
        String filePath = uploadPath + File.separator + filePart.filename();
        String subfix= filePart.filename().substring(filePart.filename().lastIndexOf(".")+1).toLowerCase();
        if(!subfix.equals("doc")&&!subfix.equals("docx"))return Mono.just(ApiResponse.error("目前只支持doc和docx格式的文件"));
        FileLog fileLog = new FileLog();
        String username = token.split("-")[0];
        fileLog.setId(0L);
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

    /**
     * Excel 文件上传并向量化入库。
     * 每 8 行数据作为一个 Document 写入向量库，保留 "列名: 值" 的结构化格式。
     */
    @PostMapping(value = "/embedding/uploadExcel", consumes = "multipart/form-data", produces = "application/json")
    public Mono<ApiResponse<String>> uploadExcel(@RequestPart("file") FilePart filePart,
                                                 @RequestParam("knowledgeBaseId") Long knowledgeBaseId,
                                                 @RequestHeader("Authorization") String token) {
        String fileName = filePart.filename();
        if (!ExcelParseHelper.isExcelFile(fileName)) {
            return Mono.just(ApiResponse.error("仅支持 .xlsx 和 .xls 格式的 Excel 文件"));
        }

        String filePath = uploadPath + File.separator + fileName;
        FileLog fileLog = new FileLog();
        String username = token.split("-")[0];
        fileLog.setId(0L);
        fileLog.setFileName(fileName);
        fileLog.setFilePath(filePath);
        fileLog.setUploadTime(new Date());
        fileLog.setKnowledgeBaseId(knowledgeBaseId);
        fileLog.setFaculty(username);
        fileLogService.saveFileLog(fileLog);

        File dest = new File(filePath);
        return filePart.transferTo(dest)
                .then(Mono.fromCallable(() -> {
                    // 使用 8 行分块解析 Excel
                    List<Document> documents = excelParseHelper.parseExcel(filePath, ExcelParseHelper.EMBEDDING_ROWS_PER_CHUNK);
                    if (documents.isEmpty()) {
                        fileLog.setProcessingState(FileLog.ProcessingState.FAILED);
                        fileLogService.saveFileLog(fileLog);
                        return ApiResponse.error("Excel 文件内容为空，无法入库");
                    }

                    // 获取知识库 tag 作为向量索引名
                    KnowledgeBase kb = fileLogService.getKnowledgeBaseById(knowledgeBaseId);
                    if (kb == null || StringUtils.isBlank(kb.getTag())) {
                        fileLog.setProcessingState(FileLog.ProcessingState.FAILED);
                        fileLogService.saveFileLog(fileLog);
                        return ApiResponse.error("未找到关联的知识库或知识库 tag 为空");
                    }
                    String tag = kb.getTag().trim();

                    // 为每个 document 补充日期元数据
                    var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    var date = sdf.format(new Date());
                    for (Document doc : documents) {
                        doc.getMetadata().put("date", date);
                        doc.getMetadata().put("fileName", fileName);
                    }

                    // 写入向量库
                    VectorStore vectorStore = vectorStoreFactory.createVectorStore(tag, tag, embeddingModel);
                    vectorStore.add(documents);
                    System.out.println("Excel 向量入库完成，共 " + documents.size() + " 个分块");

                    fileLog.setProcessingState(FileLog.ProcessingState.COMPLETED);
                    fileLogService.saveFileLog(fileLog);
                    return ApiResponse.success("Excel 入库成功，共 " + documents.size() + " 个分块");
                }))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    e.printStackTrace();
                    fileLog.setProcessingState(FileLog.ProcessingState.FAILED);
                    fileLogService.saveFileLog(fileLog);
                    return Mono.just(ApiResponse.error("Excel 入库失败：" + e.getMessage()));
                });
    }

    /**
     * 通过已上传的 Excel 文件路径进行向量化入库（与 /embedding/byDocPath 类似）。
     */
    @PostMapping("/embedding/byExcelPath")
    public ApiResponse<String> embeddingByExcelPath(@RequestParam("path") String path) {
        FileLog fileLog = fileLogService.getByFilePath(path);
        if (fileLog == null) return ApiResponse.error("未找到该文件记录");
        try {
            KnowledgeBase knowledgeBase = fileLog.getKnowledgeBase();
            if (knowledgeBase == null) {
                return ApiResponse.error("未找到关联的知识库");
            }
            String tag = knowledgeBase.getTag();
            if (StringUtils.isBlank(tag)) {
                return ApiResponse.error("知识库 tag 为空");
            }
            tag = tag.trim();

            List<Document> documents = excelParseHelper.parseExcel(path, ExcelParseHelper.EMBEDDING_ROWS_PER_CHUNK);
            if (documents.isEmpty()) {
                return ApiResponse.error("Excel 文件内容为空");
            }

            var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            var date = sdf.format(new Date());
            for (Document doc : documents) {
                doc.getMetadata().put("date", date);
            }

            VectorStore vectorStore = vectorStoreFactory.createVectorStore(tag, tag, embeddingModel);
            vectorStore.add(documents);
            System.out.println("Excel byPath 向量入库完成，共 " + documents.size() + " 个分块");

            fileLog.setProcessingState(FileLog.ProcessingState.COMPLETED);
            fileLogService.saveFileLog(fileLog);
        } catch (Exception e) {
            fileLog.setProcessingState(FileLog.ProcessingState.FAILED);
            fileLogService.saveFileLog(fileLog);
            return ApiResponse.error("Excel 入库失败：" + e.getMessage());
        }
        return ApiResponse.success("Excel 入库成功");
    }

}