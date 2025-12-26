package com.xiongdwm.ai_demo.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongdwm.ai_demo.ingest.CustomerServiceGraphService;
import com.xiongdwm.ai_demo.utils.JacksonUtil;
import com.xiongdwm.ai_demo.utils.config.Neo4jVectorStoreFactory;
import com.xiongdwm.ai_demo.utils.global.ApiResponse;
import com.xiongdwm.ai_demo.utils.global.GlobalPrompt;
import com.xiongdwm.ai_demo.utils.global.HierarchicalWordSplitHelper;
import com.xiongdwm.ai_demo.utils.global.SectionNode;
import com.xiongdwm.ai_demo.webapp.entities.FileLog;
import com.xiongdwm.ai_demo.webapp.entities.KnowledgeBase;
import com.xiongdwm.ai_demo.webapp.service.FileLogService;
import io.micrometer.common.util.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class CustomerServiceApi {
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private Neo4jVectorStoreFactory vectorStoreFactory;
    @Autowired
    private FileLogService fileLogService;
    @Autowired
    private HierarchicalWordSplitHelper hierarchicalWordSplitHelper;
    @Autowired
    private CustomerServiceGraphService customerServiceGraphService;
    @Autowired
    private ChatContextManager chatContextManager;
    @Autowired
    private OllamaChatModel ollamaChatModel;
    @Autowired
    private Neo4jVectorStoreFactory vectorStore;

    @Value("${file.upload.path.cs}")
    private String uploadPath;
    // 可选：图片保存目录；为空则默认存到 uploadPath/doc-pics 下
    @Value("${pic.doc.path.cs}")
    private String picSaveDir;

    private static final String CUSTOMER_SERVICE_TAG = "customer_service";
    private static final ObjectMapper MAPPER = new ObjectMapper();


    /**
     * 上传客服手册：自动绑定到 tag=customer_service 的知识库，没有则创建。
     */
    @PostMapping(value = "/customer-service/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ApiResponse<String>> uploadCustomerManual(@RequestPart("file") FilePart filePart,
                                                          @RequestHeader(value = "Authorization", required = false) String token) {
        String filename = filePart.filename();
        String suffix = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!suffix.equals("doc") && !suffix.equals("docx")) {
            return Mono.just(ApiResponse.error("目前只支持doc和docx格式的文件"));
        }
        KnowledgeBase kb = fileLogService.getKnowledgeBaseByTag(CUSTOMER_SERVICE_TAG);
        if (kb == null) {
            kb = new KnowledgeBase();
            kb.setName("客服知识库");
            kb.setDescription("用于客服手册的向量检索");
            kb.setTag(CUSTOMER_SERVICE_TAG);
            fileLogService.saveKnowledgeBase(kb);
            vectorStore.createVectorIndex(CUSTOMER_SERVICE_TAG,CUSTOMER_SERVICE_TAG,768,"embedding","cosine");
            kb = fileLogService.getKnowledgeBaseByTag(CUSTOMER_SERVICE_TAG);
        }
        String filePath = uploadPath + File.separator + filename;
        File dest = new File(filePath);

        FileLog fileLog = new FileLog();
        fileLog.setId(0L);
        fileLog.setFileName(filename);
        fileLog.setFilePath(filePath);
        fileLog.setUploadTime(new Date());
        fileLog.setKnowledgeBaseId(kb.getId());
        String username = (token != null && token.contains("-")) ? token.split("-")[0] : "anonymous";
        fileLog.setFaculty(username);
        fileLog.setProcessingState(FileLog.ProcessingState.PENDING);
        fileLogService.saveFileLog(fileLog);

        return filePart.transferTo(dest)
                .then(Mono.fromCallable(() -> ApiResponse.success("上传成功: " + filename)))
                .onErrorResume(e -> Mono.just(ApiResponse.error("上传失败: " + e.getLocalizedMessage())));
    }

    /**
     * 将指定路径的客服手册解析（层级+步骤+图片），写入 customer_service 向量库，并构建图谱。
     */
    @PostMapping("/customer-service/embedding/byDocPath")
    public ApiResponse<String> buildCustomerServiceEmbedding(@RequestParam("path") String path) {
        FileLog fileLog = fileLogService.getByFilePath(path);
        if (fileLog == null) return ApiResponse.error("未找到对应文件，请先上传手册。");
        KnowledgeBase knowledgeBase = fileLog.getKnowledgeBase();
        if (knowledgeBase == null || !CUSTOMER_SERVICE_TAG.equalsIgnoreCase(knowledgeBase.getTag())) {
            return ApiResponse.error("该文件未绑定客服知识库(customer_service)，请检查知识库配置。");
        }
        String tag = knowledgeBase.getTag();
        try {
            String imageDir = StringUtils.isBlank(picSaveDir) ? (uploadPath + File.separator + "doc-pics") : picSaveDir;
            List<SectionNode> nodes = hierarchicalWordSplitHelper.parseHierarchy(path, imageDir, null, 1000);

            // 写向量库：以步骤为主粒度，同时补充 Section 级文档
            VectorStore myVectorStore = vectorStoreFactory.createVectorStore(tag, tag, embeddingModel);
            var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            var date = sdf.format(new Date());
            List<Document> documents = new ArrayList<>();
            for (SectionNode n : nodes) {
                if (n.getSteps() != null && !n.getSteps().isEmpty()) {
                    for (String stepJson : n.getSteps()) {
                        try {
                            Map<String, Object> step = MAPPER.readValue(stepJson, new TypeReference<Map<String, Object>>(){});
                            int idx = (step.get("index") instanceof Number) ? ((Number) step.get("index")).intValue() : 0;
                            String text = Objects.toString(step.get("text"), "");
                            @SuppressWarnings("unchecked")
                            List<String> imgs = (List<String>) step.getOrDefault("imageUrls", Collections.emptyList());
                            String content = (n.getTitle() == null ? "" : ("[" + n.getTitle() + "]\n")) + text;
                            Map<String, Object> md = new HashMap<>();
                            md.put("date", date);
                            md.put("type", "manual_step");
                            md.put("sectionId", n.getId());
                            md.put("sectionTitle", n.getTitle());
                            md.put("level", n.getLevel());
                            md.put("stepIndex", idx);
                            md.put("filePath", path);
                            md.put("imageUrls", imgs);
                            documents.add(new Document(content, md));
                        } catch (Exception ignore) { }
                    }
                }
                // 补充 Section 级文档，增强召回
                if (n.getText() != null && !n.getText().isEmpty()) {
                    String content = (n.getTitle() == null ? "" : ("[" + n.getTitle() + "]\n")) + n.getText();
                    Map<String, Object> md = new HashMap<>();
                    md.put("date", date);
                    md.put("type", "manual_section");
                    md.put("sectionId", n.getId());
                    md.put("sectionTitle", n.getTitle());
                    md.put("level", n.getLevel());
                    md.put("filePath", path);
                    md.put("imageUrls", n.getImageUrls());
                    documents.add(new Document(content, md));
                }
            }
            if (!documents.isEmpty()) myVectorStore.add(documents);

            // 构建图谱
            customerServiceGraphService.buildGraph(nodes, path);

            fileLog.setProcessingState(FileLog.ProcessingState.COMPLETED);
            fileLogService.saveFileLog(fileLog);
            return ApiResponse.success("已解析并入库向量/图谱，文档数：" + documents.size());
        } catch (Exception e) {
            fileLog.setProcessingState(FileLog.ProcessingState.FAILED);
            fileLogService.saveFileLog(fileLog);
            return ApiResponse.error("处理失败：" + e.getLocalizedMessage());
        }
    }

    /**
     * 新增离散问答对到客服知识库（用于FAQ/样例问答）
     */
    @PostMapping("/customer-service/qa/add")
    public ApiResponse<String> addFAQ(@RequestParam("question") String question,
                                      @RequestParam("answer") String answer) {
        try {
            VectorStore vs = vectorStoreFactory.createVectorStore(CUSTOMER_SERVICE_TAG, CUSTOMER_SERVICE_TAG, embeddingModel);
            var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            var date = sdf.format(new Date());
            String text = "问：" + question + "\n答：" + answer;
            Map<String, Object> md = new HashMap<>();
            md.put("type", "faq");
            md.put("date", date);
            vs.add(List.of(new Document(text, md)));
            return ApiResponse.success("已写入FAQ问答");
        } catch (Exception e) {
            return ApiResponse.error("写入失败：" + e.getMessage());
        }
    }

    /**
     * 客服问答（流式）：从 customer_service 检索，保留图片 URL，并在回答中提示展示。
     *
     * 改进：
     * - 将阻塞检索/图谱操作放到 boundedElastic 线程池执行，避免阻塞 Reactor I/O
     * - 使用 Mono 构建 prompt，然后在 Flux.create 中对 Ollama 的流式响应进行订阅
     * - 使用 FluxSink.OverflowStrategy.BUFFER 防止背压导致取消
     * - 不再注册全局/共享 sink，避免其他请求覆盖或取消当前会话
     */
    @PostMapping("/customer-service/streaming/chat")
    public Flux<String> streamingCustomerChat(@RequestParam("message") String message,
                                              @RequestHeader(value = "chat-id", required = false) String chatId) {
        if (StringUtils.isBlank(chatId)) chatId = UUID.randomUUID().toString();
        String conversationId = chatId + "-cs-" + System.currentTimeMillis();

        final String topicId = chatId + "-cs";

        if (StringUtils.isBlank(message)) {
            // 立即返回一条错误并完成
            return Flux.create(sink -> {
                JacksonUtil.toJsonString(new ConversationContext("消息不能为空", conversationId))
                        .filter(s -> !s.isBlank())
                        .ifPresent(s -> sink.next(s + "</chunk>"));
                sink.complete();
            }, FluxSink.OverflowStrategy.BUFFER);
        }

        Mono<String> promptMono = Mono.fromCallable(() -> {
            // 检索向量库（可能阻塞）
            VectorStore vs = vectorStoreFactory.createVectorStore(CUSTOMER_SERVICE_TAG, CUSTOMER_SERVICE_TAG, embeddingModel);
            List<Document> retrieved = vs.similaritySearch(SearchRequest.builder()
                    .query(message)
                    .similarityThreshold(0.75f)
                    .topK(12)
                    .build());

            // 准备知识上下文（含图片URL + 图谱路径）
            StringBuilder kbBuilder = new StringBuilder();
            kbBuilder.append("##相关知识（来自客服手册）：\n");

            // 利用图谱查询每个文档对应的手册路径
            Set<String> sectionIds = new LinkedHashSet<>();
            if( null==retrieved||retrieved.isEmpty()) {
                kbBuilder.append("无相关知识。\n");
                return kbBuilder.toString();
            }
            for (Document d : retrieved) {
                Object sid = d.getMetadata().get("sectionId");
                if (sid instanceof String s && !s.isEmpty()) sectionIds.add(s);
            }
            List<Map<String, Object>> sectionPaths = customerServiceGraphService.getSectionPaths(new ArrayList<>(sectionIds));
            Map<String, String> sectionIdToPath = new HashMap<>();
            for (Map<String, Object> sp : sectionPaths) {
                String sid = Objects.toString(sp.get("sectionId"), "");
                String manual = Objects.toString(sp.get("manualFilePath"), "");
                @SuppressWarnings("unchecked")
                List<String> titles = (List<String>) sp.getOrDefault("sectionPath", Collections.emptyList());
                String pathStr = (manual == null ? "" : manual) + (titles.isEmpty() ? "" : (" > " + String.join(" > ", titles)));
                sectionIdToPath.put(sid, pathStr);
            }

            for (Document d : retrieved) {
                String text = d.getText();
                Object imgsObj = d.getMetadata().get("imageUrls");
                @SuppressWarnings("unchecked")
                List<String> imgs = (imgsObj instanceof List) ? (List<String>) imgsObj : Collections.emptyList();
                String sid = Objects.toString(d.getMetadata().get("sectionId"), "");
                String pathStr = sectionIdToPath.getOrDefault(sid, "");
                kbBuilder.append("###位置：").append(pathStr).append("\n");
                kbBuilder.append("###片段：\n").append(text).append("\n");
                if (!imgs.isEmpty()) {
                    kbBuilder.append("###图片：\n");
                    int k = 1;
                    for (String url : imgs) {
                        kbBuilder.append("- 步骤" + k + "图片：<url>").append(url).append("</url>").append("\n");
                        k++;
                    }
                }
            }

            List<String> context = chatContextManager.getAllContextFromCache(topicId);
            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("你是雄博科技研发的企业客服助手。\n");
            promptBuilder.append("- 严格优先使用提供的手册知识回答。\n");
            promptBuilder.append("- 当步骤涉及图片，请在相应步骤后附上图片URL。\n");
            promptBuilder.append("- 严格遵守图片路径格式要求，格式为<url>图片路径</url> 图片格式示例：<url>b0b672a1-c542-41fe-b8fa-e4168247d363_1765963566571.png</url>。\n");
            promptBuilder.append("- 回答时不允许篡改图片路径。\n");
            promptBuilder.append("- 若知识不足以回答，再补充常识。\n");
            if (!context.isEmpty()) {
                promptBuilder.append("##历史上下文：\n");
                context.forEach(promptBuilder::append);
            }
            promptBuilder.append("##用户问题：\n").append(message).append("\n");
            promptBuilder.append(kbBuilder);
            promptBuilder.append("##请用中文，给出清晰分步回答；如有图片URL，请在对应步骤行内给出。\n");
            promptBuilder.append("##请结合问题背景和检索到的知识进行回答，确保内容准确完整，语气礼貌。\n");

            return promptBuilder.toString();
        }).subscribeOn(Schedulers.boundedElastic())
          .timeout(Duration.ofSeconds(10))
          .retryWhen(Retry.fixedDelay(1, Duration.ofMillis(200)));

        return promptMono.flatMapMany(prompt -> Flux.create(sink -> {
            StringBuilder fullAnswer = new StringBuilder();
            Flux<ChatResponse> stream = ollamaChatModel.stream(new Prompt(prompt))
                    .timeout(Duration.ofSeconds(60))
                    .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(1)));
            Disposable sub = stream.map(r -> r.getResult().getOutput().getText())
                    .doOnNext(chunk -> {
                        if (chunk == null || chunk.isBlank() || sink.isCancelled()) return;
                        System.out.print(chunk);
                        fullAnswer.append(chunk);
                        JacksonUtil.toJsonString(new ConversationContext(chunk, conversationId))
                                .filter(s -> !s.isBlank())
                                .ifPresent(s -> sink.next(s + "</chunk>"));
                    })
                    .doOnComplete(() -> {
                        String answer = ChatUtils.extractAnswerOnly(fullAnswer.toString());
                        if (!answer.isEmpty()) chatContextManager.putContextToCache(topicId, message, answer);
                        sink.complete();
                    })
                    .doOnError(err -> {
                        JacksonUtil.toJsonString(new ConversationContext("【系统】发生错误：" + err.getMessage(), conversationId))
                                .filter(s -> !s.isBlank())
                                .ifPresent(s -> sink.next(s + "</chunk>"));
                        sink.complete();
                    })
                    .subscribe();

            sink.onCancel(() -> {
                if (!sub.isDisposed()) sub.dispose();
            });
        }, FluxSink.OverflowStrategy.BUFFER));
    }
}
