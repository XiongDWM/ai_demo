package com.xiongdwm.ai_demo.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import com.xiongdwm.ai_demo.utils.JacksonUtil;
import com.xiongdwm.ai_demo.utils.config.Neo4jVectorStoreFactory;
import com.xiongdwm.ai_demo.utils.global.ApiResponse;
import com.xiongdwm.ai_demo.utils.global.GlobalPrompt;
import com.xiongdwm.ai_demo.utils.global.WordSplitHelper;
import com.xiongdwm.ai_demo.utils.global.HierarchicalWordSplitHelper;
import com.xiongdwm.ai_demo.utils.global.SectionNode;
import com.xiongdwm.ai_demo.ingest.ImageCaptionClient;
import com.xiongdwm.ai_demo.utils.global.Neo4jIndexer;

import io.micrometer.common.util.StringUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
public class ChatApi {
    @Autowired
    @Qualifier("dashscopeChat")
    private ChatModel dashscopeChatModel;
    @Autowired
    private ChatContextManager chatContextManager;
    @Autowired
    @Qualifier("ollamaEmbedding")
    private EmbeddingModel embeddingModel;
    @Autowired
    private Neo4jVectorStoreFactory vectorStoreFactory;
    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private ImageCaptionClient imageCaptionClient;
    @Autowired
    private Neo4jIndexer neo4jIndexer;
    @Autowired
    private HierarchicalWordSplitHelper hierarchicalWordSplitHelper;

    @PostMapping("/streaming/chat/baseKnowledge")
    public Flux<String> sinkFlux(@RequestParam("message") String message,
            @RequestHeader(value = "chat-id", required = false) String chatId,
            @RequestParam(value = "knowledge", required = false) String knowledge,
            @RequestParam(value = "fileName", required = false) String fileName,
            @SessionAttribute(value="user",required = false) String auth,
            @RequestParam(value = "pictureName", required = false) String pictureName) {

        String conversationId = chatId + "-" + System.currentTimeMillis();
        System.out.println("cookie:" + auth);
        return Flux.create(sink -> {
            System.out.println("传入cvid：" + conversationId);
            chatContextManager.registerSink(conversationId, sink);
            if (message == null || message.isEmpty()) {
                sink.next(
                        JacksonUtil.toJsonString(new ConversationContext("消息不能为空", conversationId)).orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                sink.complete();
                return;
            }
            if (chatId == null || chatId.isEmpty()) {
                sink.next(JacksonUtil.toJsonString(new ConversationContext("chat-id不能为空", conversationId)).orElse(ConversationContext.getEmptyContextJsonString())
                        + "</chunk>");
                sink.complete();
                return;
            }
            var isUploaded = (fileName != null && !fileName.isEmpty())||(pictureName != null && !pictureName.isEmpty());
            var isBaseKnowledge = (knowledge != null && !knowledge.isEmpty());
            var isDb=(knowledge!=null&&!knowledge.isEmpty()&&knowledge.contains("q->sql")&&knowledge.contains("db_description"));
            var contexts = chatContextManager.getLatestWithIntents(chatId);
            System.out.println("上轮对话：" + contexts);
            sink.next(JacksonUtil.toJsonString(new ConversationContext("【系统】意图识别中...", conversationId)).orElse(ConversationContext.getEmptyContextJsonString())
                    + "</chunk>");
            System.out.println("意图识别中...");
            AtomicBoolean cancelled = new AtomicBoolean(false);
            final Disposable[] disposable = new Disposable[2];
            Disposable heartbeat = Flux.interval(java.time.Duration.ofSeconds(10))
                 .subscribe(tick -> {
                    sink.next(JacksonUtil.toJsonString(
                    new ConversationContext("", conversationId)).orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
            });
            sink.onCancel(() -> {
                cancelled.set(true);
                if(heartbeat != null && !heartbeat.isDisposed()) {
                    heartbeat.dispose();
                }
                if (disposable[0] != null && !disposable[0].isDisposed()) {
                    disposable[0].dispose();
                }
                if (disposable[1] != null && !disposable[1].isDisposed()) {
                    disposable[1].dispose();
                }
                sink.complete();
            });
            disposable[0] = intentMsgAsync(message, contexts,isUploaded,isBaseKnowledge,isDb)
                    .subscribe(intent -> {
                        sink.next(JacksonUtil.toJsonString(new ConversationContext("【系统】识别意图：" + intent, conversationId))
                                .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                        System.out.println("已识别意图：" + intent);
                        var topicId = chatId + "-" + intent;
                        switch (intent) {
                            case "1":
                                sink.next(JacksonUtil
                                        .toJsonString(new ConversationContext("【系统】数据库结构检索中...", conversationId)).orElse(ConversationContext.getEmptyContextJsonString())
                                        + "</chunk>");
                                List<Document> results = dbDescriptionGenerate(message);
                                if (results.isEmpty()) {
                                    sink.next(JacksonUtil
                                            .toJsonString(new ConversationContext("【系统】未找到相关数据库结构描述", conversationId))
                                            .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                                    sink.complete();
                                    return;
                                }
                                sink.next(JacksonUtil.toJsonString(new ConversationContext("【系统】SQL生成中", conversationId))
                                        .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                                System.out.println("SQL生成中...");
                                sqlGenerateAsync(results, message, topicId)
                                        .subscribe(sql -> {
                                            sink.next(JacksonUtil
                                                    .toJsonString(
                                                            new ConversationContext("【系统】已生成SQL：" + sql, conversationId))
                                                    .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                                            System.out.println("已生成SQL：" + sql);
                                            if (sql.isEmpty()) {
                                                sink.next(JacksonUtil
                                                        .toJsonString(
                                                                new ConversationContext("【系统】生成SQL失败", conversationId))
                                                        .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                                                sink.complete();
                                                return;
                                            }
                                            sink.next(JacksonUtil
                                                    .toJsonString(
                                                            new ConversationContext("【系统】SQL执行中...", conversationId))
                                                    .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                                            System.out.println("SQL执行中...");
                                            String sqlResult = sqlExecute(sql);
                                            sink.next(JacksonUtil
                                                    .toJsonString(new ConversationContext("【系统】SQL执行完成", conversationId))
                                                    .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                                            System.out.println("SQL执行完成");
                                            sink.next(JacksonUtil
                                                    .toJsonString(
                                                            new ConversationContext("【系统】正在回答问题...", conversationId))
                                                    .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                                            dbAgentLLMAnswer(results, sqlResult, message, topicId)
                                                    .doOnNext(chunk -> {
                                                        sink.next(JacksonUtil
                                                                .toJsonString(
                                                                        new ConversationContext(chunk, conversationId))
                                                                .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                                                    })
                                                    .doOnComplete(sink::complete)
                                                    .subscribe();
                                        });
                                break;
                            case "2":
                                sink.next(JacksonUtil.toJsonString(new ConversationContext("【系统】知识库问答中...\n", conversationId))
                                        .orElse(ConversationContext.getEmptyContextJsonString())
                                        + "</chunk>");
                                List<Document> fileContent = new ArrayList<>();
                                if (fileName != null && !fileName.isEmpty()) {

                                    sink.next(JacksonUtil
                                            .toJsonString(new ConversationContext("【系统】正在解析文件内容...", conversationId))
                                            .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                                    try {
                                        // 使用分层拆分器实例方法，保持标题-子项结构并提取图片
                                        var sections = hierarchicalWordSplitHelper.parseHierarchy(fileName,
                                                "upload/images", imageCaptionClient, 1200);
                                        // 将 SectionNode 转 Document 并保留 imageUrls 在 metadata
                                        for (SectionNode s : sections) {
                                            var text = (s.getTitle() == null ? "" : s.getTitle()) + "\n" + (s.getText() == null ? "" : s.getText());
                                            var meta = new java.util.HashMap<String, Object>();
                                            if (s.getImageUrls() != null && !s.getImageUrls().isEmpty()) meta.put("imageUrls", s.getImageUrls());
                                            if (s.getSteps() != null && !s.getSteps().isEmpty()) meta.put("steps", s.getSteps());
                                            meta.put("sectionId", s.getId());
                                            meta.put("level", s.getLevel());
                                            var doc = new Document(text, meta);
                                            fileContent.add(doc);
                                        }
                                        sink.next(JacksonUtil
                                                .toJsonString(new ConversationContext("【系统】已解析文件内容", conversationId))
                                                .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                                    } catch (Exception e) {
                                        sink.next(JacksonUtil.toJsonString(
                                                new ConversationContext("文件解析失败：" + e.getMessage(), conversationId))
                                                .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                                    }
                                }
                                disposable[1] = streamingChatWithBaseKnowledge(message, topicId, fileContent,knowledge)
                                        .doOnNext(chunk -> {
                                            sink.next(JacksonUtil
                                                    .toJsonString(new ConversationContext(chunk, conversationId)).orElse(ConversationContext.getEmptyContextJsonString())
                                                    + "</chunk>");
                                        })
                                        .doOnComplete(sink::complete)
                                        .subscribe();
                                break;
                            case "3":
                                sink.next(JacksonUtil.toJsonString(new ConversationContext("【系统】闲聊中...", conversationId))
                                        .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                                disposable[1] = streamingChat(message, topicId)
                                        .doOnNext(chunk -> {
                                            sink.next(JacksonUtil
                                                    .toJsonString(new ConversationContext(chunk, conversationId)).orElse(ConversationContext.getEmptyContextJsonString())
                                                    + "</chunk>");
                                        })
                                        .doOnComplete(sink::complete)
                                        .subscribe();
                                break;
                            default:
                                System.out.println("错误意图"+intent);
                                sink.next(JacksonUtil.toJsonString(new ConversationContext("意图识别失败，请重试", conversationId))
                                        .orElse(ConversationContext.getEmptyContextJsonString()) + "</chunk>");
                                sink.complete();
                        }
                    }, error -> {
                        error.printStackTrace();
                        sink.next(JacksonUtil.toJsonString(new ConversationContext("【系统】系统异常", conversationId)).orElse(ConversationContext.getEmptyContextJsonString())
                                + "</chunk>");
                        sink.complete();
                        return;
                    });
        });
    }

    private List<Document> dbDescriptionGenerate(String message) {
        VectorStore myVectorStore = vectorStoreFactory.createVectorStore("db_description", "db_description",
                embeddingModel);
        List<Document> results = myVectorStore.similaritySearch(SearchRequest.builder()
                .query(message)
                .topK(20)
                .similarityThreshold(0.6f)
                .build());
        System.out.println("description results: " + results.size());
        return results;
    }

    private String sqlExecute(String sql) {
        // 执行sql
        var query = entityManager.createNativeQuery(sql);
        List<Object[]> resultList = query.getResultList();
        System.out.println(resultList);
        StringBuilder sb = new StringBuilder();
        for (Object[] row : resultList) {
            sb.append(java.util.Arrays.toString(row)).append("\n");
        }
        return sb.toString();
    }

    @GetMapping("/chat/dbAgentAnswer")
    public Flux<String> dbAgentLLMAnswer(List<Document> results, String sqlResult, String message, String chatId) {
        System.out.println("dbAgentLLMAnswerId: " + chatId);
        StringBuilder answerPrompt = new StringBuilder();
        answerPrompt.append("你是数据库问答助手。请结合下方数据库字段描述和SQL查询结果，用简洁自然的语言回答用户问题。\n")
                .append("###注意数字，不要编造数字\n")
                .append("###数据库字段描述：\n");
        for (Document doc : results) {
            answerPrompt.append(doc.getText()).append("\n");
        }
        answerPrompt.append("###用户问题：\n").append(message).append("\n")
                .append("###SQL查询结果：\n").append(sqlResult).append("\n")
                .append("###注意不要用数据主键作为数据的代表,需要用具体名称来代表数据条目\n")
                .append("###请用中文回答：");
        StringBuilder resultBuilder = new StringBuilder();
        return dashscopeChatModel.stream(new Prompt(answerPrompt.toString()))
                .map(chatResp -> chatResp.getResult().getOutput().getText())
                .doOnNext(resultBuilder::append).doOnComplete(() -> {
                    String result = resultBuilder.toString();
                    var answer = ChatUtils.extractAnswerOnly(result);
                    if (!answer.isEmpty()) {
                        chatContextManager.putContextToCache(chatId, message, answer);
                    }
                }).doOnCancel(() -> {
                    System.out.println("回答取消");
                });
    }

    @PostMapping("/test")
    public Mono<String> test() {
        WordSplitHelper wordSplitHelper = new WordSplitHelper();
        String filePath = "/Users/xiong/Files/ai_demo/upload/中国移动哑资源数智化转型白皮书会议纪要.docx";
        try {
            var list = wordSplitHelper.getAllText(filePath);
            System.out.println(list);
            return Mono.just(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Mono.just("Test method executed");
    }

    @PostMapping("/streaming/cancel")
    public Mono<ApiResponse<String>> cancelStreaming(@RequestParam("conversationId") String conversationId) {
        System.out.println();
        System.out.println("取消接口调用");
        chatContextManager.cancel(conversationId, "回答取消");
        return Mono.just(ApiResponse.success("已取消回答"));
    }

    private Flux<String> streamingChat(String message, String chatId) {
        System.out.println("streamingChatId: " + chatId);
        List<String> context = chatContextManager.getAllContextFromCache(chatId);
        // 构造 prompt
        var promptBuilder = new StringBuilder();
        promptBuilder.append(GlobalPrompt.IDENTITY_STRING);
        promptBuilder.append("##你是一个智能助手，用户会问你问题，你需要根据上下文和问题作出合理、自然的回答\n");
        if (context.isEmpty()) {
            promptBuilder.append("##用户当前的问题是：\n").append(message).append("\n");
        } else {
            promptBuilder.append("##用户当前的问题是：\n").append(message).append("\n");
            promptBuilder.append("##你需要结合上下文作出合理的回答\n");
            promptBuilder.append("##上下文如下：\n");
            context.forEach(promptBuilder::append);
        }
        Flux<ChatResponse> stream = dashscopeChatModel.stream(new Prompt(promptBuilder.toString()));

        // 回答并缓存问答
        StringBuilder fullAnswerBuilder = new StringBuilder();
        return stream.map(chatResp -> chatResp.getResult().getOutput().getText())
                .doOnNext(chunk -> {
                    fullAnswerBuilder.append(chunk);
                    System.out.print(chunk);
                })
                .doOnComplete(() -> {
                    // 在流完成后存储完整的问答
                    String fullAnswer = ChatUtils.extractAnswerOnly(fullAnswerBuilder.toString());
                    if (!fullAnswer.isEmpty()) {
                        chatContextManager.putContextToCache(chatId, message, fullAnswer);
                    }
                }).doOnCancel(() -> {
                    System.out.println();
                    System.out.println("回答取消");
                });
    }

    private Flux<String> streamingChatWithBaseKnowledge(String message, String chatId, List<Document> fileContent,
            String knowledge) {
        List<String> context = chatContextManager.getAllContextFromCache(chatId);

        return Mono.fromCallable(() -> {
                    final List<Document> documents = new ArrayList<>();
                    String knowledgeChosen=knowledge;
                    if (StringUtils.isBlank(knowledgeChosen) && fileContent.isEmpty()) {
                        knowledgeChosen = "base_knowledge";
                        var vectorStore = vectorStoreFactory.createVectorStore("base_knowledge", "base_knowledge", embeddingModel);
                        documents.addAll(Objects.requireNonNull(vectorStore.similaritySearch(SearchRequest.builder()
                                .query(message)
                                .similarityThreshold(0.8f)
                                .topK(10)
                                .build())));
                    } else {
                        for (String split : knowledge.split(",")) {
                            var vectorStore = vectorStoreFactory.createVectorStore(split, split, embeddingModel);
                            List<Document> searchResults = vectorStore.similaritySearch(SearchRequest.builder()
                                    .query(message)
                                    .similarityThreshold(0.8f)
                                    .topK(10)
                                    .build());

                            // 多个知识库知识重新排序
                            if (searchResults != null && !searchResults.isEmpty()) {
                                documents.addAll(searchResults);
                                List<Document> sortedDocuments = documents.stream().sorted((d1, d2) -> Double.compare(d2.getScore(), d1.getScore())).toList();
                                List<Document> topDocuments = sortedDocuments.size() > 10 ? sortedDocuments.subList(0, 10) : sortedDocuments;
                                documents.clear();
                                documents.addAll(topDocuments);
                            }
                        }
                    }
                    if (!fileContent.isEmpty()) {
                        documents.addAll(fileContent);
                    }
                    return documents;
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(documents -> {
                    var promptBuilder = new StringBuilder();
//                    promptBuilder.append(GlobalPrompt.IDENTITY_STRING);
                    promptBuilder.append("##你是一个智能RAG助手，用户会问你问题，你需要根据上下文、知识库内容和问题作出合理、自然的回答\n");
                    if (context.isEmpty()) {
                        promptBuilder.append("##用户当前的问题是：\n").append(message).append("\n");
                    } else {
                        promptBuilder.append("##用户当前的问题是：\n").append(message).append("\n");
//                        promptBuilder.append("##你需要结合上下文作出合理、自然的回答\n");
                        promptBuilder.append("##上下文如下：\n");
                        context.forEach(promptBuilder::append);
                        promptBuilder.append("##如果上下文内容与这次问题无关，忽略上下文\n");
                    }
                    if (!knowledge.isEmpty()) {
//                        promptBuilder.append("##你需要结合知识库作出合理、自然的回答\n");
                        promptBuilder.append("##如果知识库内容无法完全回答，可以补充常识。\n");
                        promptBuilder.append("##知识库内容如下：\n");
                        for (Document doc : documents) {
                            promptBuilder.append("##").append(doc.getText()).append("\n");
                        }
                    }
                    DashScopeChatOptions options=DashScopeChatOptions.builder()
                            .enableThinking(true)
                            .model("qwen-plus")
                            .build();
                    Prompt prompt = new Prompt(promptBuilder.toString(),options);

                    Flux<ChatResponse> stream = dashscopeChatModel.stream(prompt);

                    StringBuilder fullAnswerBuilder = new StringBuilder();
                    return stream.map(chatResp -> chatResp.getResult().getOutput().getText())
                            .doOnNext(chunk->{
                                fullAnswerBuilder.append(chunk);
                                System.out.print(chunk);
                            })
                            .doOnComplete(() -> {
                                String fullAnswer = ChatUtils.extractAnswerOnly(fullAnswerBuilder.toString());
                                if (!fullAnswer.isEmpty()) {
                                    chatContextManager.putContextToCache(chatId, message, fullAnswer);
                                }
                            }).doOnCancel(() -> {
                                System.out.println("回答取消");
                            });
                });
    }

    private Mono<String> intentMsgAsync(String message, List<String> contexts,boolean isUploaded,boolean isBaseKnowledge,boolean isDb) {
        if(isDb)return Mono.just("1");
        if(isUploaded||isBaseKnowledge)return Mono.just("2");
        StringBuilder prompt = new StringBuilder();
        prompt.append("##你是一个智能意图识别助手。请根据用户的历史对话上下文和当前问题，判断其意图属于以下哪一类，只返回编号，不要解释：\n")
                .append(IntentsEnum.print())
                .append("##不允许添加其他意图，并且只返回提供意图类别对应的编号1、2或3 \n")
                .append("##如果用户问题无法归类到以上意图，请返回数字0\n")
                .append("##回答只能是0、1、2或3\n")
                .append("##必须返回0、1、2或3其中一个数字，不能返回空白字符串\n");
        if (contexts != null && !contexts.isEmpty()) {
            prompt.append("##历史上下文如下：\n");
            contexts.forEach(c -> prompt.append(c).append("\n"));
        }
        prompt.append("##当前问题：").append(message).append("\n");
        Prompt promptWithModelChose = new Prompt(prompt.toString());
        return Mono.fromCallable(() -> dashscopeChatModel.call(promptWithModelChose).getResult().getOutput().getText())
                .subscribeOn(Schedulers.boundedElastic())
                .doOnCancel(() -> {
                    System.out.println("取消意图识别");
                })
                .map(ChatUtils::extractAnswerOnly);
    }

    // SQL生成流式收集
    private Mono<String> sqlGenerateAsync(List<Document> results, String message, String topicId) {
        var topic = "sql-" + topicId;
        StringBuilder prompt = new StringBuilder();
        prompt.append("###你是SQL生成助手。请根据下方数据库结构描述、用户问题以及上轮回答, 直接生成对应的SQL语句, 只输出SQL, 不允许解释。\n")
                .append("###数据库结构描述：\n");
        for (Document doc : results) {
            prompt.append(doc.getText()).append("\n");
        }

        var context = chatContextManager.getLatest(topic);
        if (StringUtils.isNotBlank(context)) {
            System.out.println(context);
            prompt.append("###上轮回答：\n").append(context).append("\n");
        } else {
            prompt.append("###上轮回答：\n").append("无\n");
        }
        prompt.append("###注意数字，不要编造数字\n")
                .append("###注意SQL语句的正确性\n")
                .append("###注意SQL语句的可读性\n")
                .append("###注意SQL语句的性能\n")
                .append("###用户问题：\n")
                .append(message).append("\n")
                .append("###SQL:\n");
        Prompt promptWithModelChose = new Prompt(prompt.toString(), ChatOptions.builder()
                .model("qwen3:1.7b")
                .build());
        return dashscopeChatModel.stream(promptWithModelChose)
                .map(chatResp -> chatResp.getResult().getOutput().getText())
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .map(ChatUtils::extractAnswerOnly)
                .doOnSuccess(sql -> {
                    if (StringUtils.isNotBlank(sql)) {
                        chatContextManager.putContextToCache(topic, message, sql);
                    }
                })
                .doOnError(error -> {
                    System.err.println("Error during SQL generation: " + error.getMessage());
                });
    }

}
