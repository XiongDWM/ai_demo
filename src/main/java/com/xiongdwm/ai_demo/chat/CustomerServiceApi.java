package com.xiongdwm.ai_demo.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongdwm.ai_demo.ingest.CustomerServiceGraphService;
import com.xiongdwm.ai_demo.ingest.EmbeddingService;
import com.xiongdwm.ai_demo.utils.JacksonUtil;
import com.xiongdwm.ai_demo.utils.global.*;
import com.xiongdwm.ai_demo.webapp.entities.FileLog;
import com.xiongdwm.ai_demo.webapp.entities.KnowledgeBase;
import com.xiongdwm.ai_demo.webapp.service.FileLogService;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
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
import reactor.core.scheduler.Scheduler;
import reactor.util.retry.Retry;

import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;

@RestController
public class CustomerServiceApi {
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
    private EmbeddingService embeddingService;

    @Resource(name = "aiScheduler")
    private Scheduler scheduler;

    private final Semaphore modelSemaphore = new Semaphore(6);

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
            var insertCheck = embeddingService.createIndex(CUSTOMER_SERVICE_TAG, CUSTOMER_SERVICE_TAG, 768, "embedding", "cosine");
            if (!insertCheck) return Mono.just(ApiResponse.error("自动知识库创建失败，请稍后重试"));
            kb = fileLogService.getKnowledgeBaseByTag(CUSTOMER_SERVICE_TAG);
        }
        String filePath = uploadPath + File.separator + filename;
        File dest = new File(filePath);

        FileLog fileLog = new FileLog();
//        fileLog.setId(0L);
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
            VectorStore myVectorStore = embeddingService.vectorStore(tag, tag);
            var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            var date = sdf.format(new Date());
            List<Document> documents = new ArrayList<>();
            for (SectionNode n : nodes) {
                if (n.getSteps() != null && !n.getSteps().isEmpty()) {
                    for (String stepJson : n.getSteps()) {
                        try {
                            Map<String, Object> step = MAPPER.readValue(stepJson, new TypeReference<Map<String, Object>>() {
                            });
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
                        } catch (Exception ignore) {
                        }
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
     * 客服问答（流式）：从 customer_service 检索，保留图片 URL，并在回答中提示展示。
     * <p>
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

        Mono<String> promptMono = Mono.fromCallable(() -> buildPrompt(message, topicId))
                .subscribeOn(scheduler)
                .timeout(Duration.ofSeconds(10))
                .retryWhen(Retry.fixedDelay(1, Duration.ofMillis(200)));

        return Flux.usingWhen(
                Mono.fromCallable(() -> {
                    if (!modelSemaphore.tryAcquire()) {
                        throw new IllegalStateException("并发已达上限，请稍后重试");
                    }
                    return Boolean.TRUE;
                }),
                acquired -> promptMono.flatMapMany(prompt -> Flux.<String>create(sink -> {
                    StringBuilder fullAnswer = new StringBuilder();

                    Flux<ChatResponse> stream = ollamaChatModel.stream(new Prompt(prompt))
                            .subscribeOn(scheduler) // 把模型流也放到专用线程池
                            .timeout(Duration.ofSeconds(60))
                            .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(1)));

                    Disposable sub = stream.map(r -> r.getResult().getOutput().getText())
                            .doOnNext(chunk -> {
                                if (chunk == null || chunk.isBlank() || sink.isCancelled()) return;
                                fullAnswer.append(chunk);
                                System.out.print(chunk);
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
                }, FluxSink.OverflowStrategy.BUFFER)), acquired -> Mono.fromRunnable(modelSemaphore::release)
        ).onErrorResume(ex -> Flux.<String>create(sink -> {
            String msg = ex.getMessage() == null ? "系统繁忙，请稍后重试" : ("【系统】" + ex.getMessage());
            JacksonUtil.toJsonString(new ConversationContext(msg, conversationId))
                    .filter(s -> !s.isBlank())
                    .ifPresent(s -> sink.next(s + "</chunk>"));
            sink.complete();
        }, FluxSink.OverflowStrategy.BUFFER));
    }

    private String buildPrompt(String message, String topicId) {
        VectorStore vs = embeddingService.vectorStore(CUSTOMER_SERVICE_TAG, CUSTOMER_SERVICE_TAG);
        List<Document> retrieved = vs.similaritySearch(SearchRequest.builder()
                .query(message)
                .similarityThreshold(0.9f)
                .topK(12)
                .build());
        var heading = "##相关知识（来自客服手册）：\n";
        // 准备知识上下文（含图片URL + 图谱路径）
        var promptKnowledge = embeddingService.graphResult2Prompt(heading, retrieved);
        System.out.println("=====================检索到的知识=========================");
        System.out.println(promptKnowledge.length());
        System.out.println("========================================================");

        StringBuilder faqSection=null;
        try {
            String faqTag = CUSTOMER_SERVICE_TAG + "_faq";
            VectorStore faqVs = embeddingService.vectorStore(faqTag, faqTag);
            List<Document> faqRetrieved = faqVs.similaritySearch(SearchRequest.builder()
                    .query(message)
                    .similarityThreshold(0.70f)
                    .topK(3)
                    .build());
            System.out.println(faqRetrieved);
            faqSection = new StringBuilder();
            if (!faqRetrieved.isEmpty()) {
                faqSection.append("##相关FAQ（常见问答）：\n");
                Set<String> seen = new LinkedHashSet<>();
                for (Document d : faqRetrieved) {
                    String txt = d.getText();
                    if (txt == null || txt.isBlank()) continue;
                    if (seen.add(txt.trim())) {
                        faqSection.append(txt.trim()).append("\n\n");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        System.out.println("=====================检索到的FAQ=========================");

        List<String> context = chatContextManager.getAllContextFromCache(topicId);
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你是雄博科技研发的企业客服助手。\n");
        promptBuilder.append("- 严格优先使用提供的手册知识以及常见问答内容回答。\n");
        promptBuilder.append("- 当步骤自身包含图片时，必须在相应步骤后附上图片URL。\n");
        promptBuilder.append("- 若步骤本身没有图片，则不得添加无关图片URL。\n");
        promptBuilder.append("- 严格遵守图片路径格式要求，格式为<url>图片路径</url> 图片格式示例：<url>b0b672a1-c542-41fe-b8fa-e4168247d363_1765963566571.png</url>。\n");
        promptBuilder.append("- 回答时不允许篡改图片路径。\n");
        promptBuilder.append("- 若知识不足以回答，再补充常识。\n");
        if (!context.isEmpty()) {
            promptBuilder.append("##历史上下文：\n");
            context.forEach(promptBuilder::append);
        }
        promptBuilder.append("##用户问题：\n").append(message).append("\n");
        promptBuilder.append(promptKnowledge);
        if(faqSection!=null&&!faqSection.isEmpty())promptBuilder.append(faqSection.toString()).append("\n");
        promptBuilder.append("##请用中文，给出清晰分步回答；仅使用与问题相关的手册/FAQ内容；如步骤自身包含图片，请在对应步骤行内给出图片URL；否则不得添加图片URL。\n");
        promptBuilder.append("##请结合问题背景和检索到的知识进行回答，确保内容准确完整，语气礼貌。\n");
        return promptBuilder.toString();
    }
}
