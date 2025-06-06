package com.xiongdwm.ai_demo.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.ai_demo.utils.config.Neo4jVectorStoreFactory;
import com.xiongdwm.ai_demo.utils.global.GlobalPrompt;
import com.xiongdwm.ai_demo.utils.global.WordSplitHelper;

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
    private OllamaChatModel ollamaChatModel;
    @Autowired
    private ChatContextManager chatContextManager;
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private Neo4jVectorStoreFactory vectorStoreFactory;
    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/streaming/chat/baseKnowledge")
    public Flux<String> sinkFlux(@RequestParam("message") String message,
            @RequestHeader(value = "chat-id", required = false) String chatId,
            @RequestParam(value = "fileName", required = false) String fileName,
            @RequestParam(value = "pictureName", required = false) String pictureName) {
        return Flux.create(sink -> {
            if (message == null || message.isEmpty()) {
                sink.next("消息不能为空");
                sink.complete();
                return;
            }
            if (chatId == null || chatId.isEmpty()) {
                sink.next("chat-id不能为空");
                sink.complete();
                return;
            }

            var contexts = chatContextManager.getLatestWithIntents(chatId);
            System.out.println("上轮对话：" + contexts);
            sink.next("【系统】意图识别中...\n");
            System.out.println("意图识别中...");
            AtomicBoolean cancelled = new AtomicBoolean(false);
            final Disposable[] disposable = new Disposable[2];
            sink.onCancel(() -> {
                cancelled.set(true);
                if (disposable[0] != null && !disposable[0].isDisposed()) {
                    disposable[0].dispose();
                }
                if (disposable[1] != null && !disposable[1].isDisposed()) {
                    disposable[1].dispose();
                }
                sink.complete();
            });
            disposable[0] = intentMsgAsync(message, contexts)
                    .subscribe(intent -> {
                        sink.next("【系统】已识别意图：" + intent + "\n");
                        System.out.println("已识别意图：" + intent);
                        var topicId = chatId + "-" + intent;
                        switch (intent) {
                            case "1":
                                sink.next("【系统】数据库结构检索中...\n");
                                List<Document> results = dbDescriptionGenerate(message);
                                if (results.isEmpty()) {
                                    sink.next("没有找到相关数据");
                                    sink.complete();
                                    return;
                                }
                                sink.next("【系统】SQL生成中...\n");
                                System.out.println("SQL生成中...");
                                sqlGenerateAsync(results, message, topicId)
                                        .subscribe(sql -> {
                                            sink.next("【系统】已生成SQL：" + sql + "\n");
                                            System.out.println("已生成SQL：" + sql);
                                            if (sql.isEmpty()) {
                                                sink.next("生成sql失败");
                                                sink.complete();
                                                return;
                                            }
                                            sink.next("【系统】SQL执行中...\n");
                                            System.out.println("SQL执行中...");
                                            String sqlResult = sqlExecute(sql);
                                            sink.next("【系统】SQL执行完成\n");
                                            System.out.println("SQL执行完成");
                                            sink.next("【系统】答案生成中...\n");
                                            dbAgentLLMAnswer(results, sqlResult, message, topicId)
                                                    .doOnNext(sink::next)
                                                    .doOnComplete(sink::complete)
                                                    .subscribe();
                                        });
                                break;
                            case "2":
                                sink.next("【系统】知识库问答中...\n");
                                List<String> fileContent = new ArrayList<>();
                                if (fileName != null && !fileName.isEmpty()) {
                                    sink.next("【系统】解析文件中...\n");
                                    try {
                                        fileContent = WordSplitHelper.splitByParagraphs(fileName);
                                        System.out.println("fileContent size: " + fileContent.size());
                                        sink.next("【系统】已解析文件内容 \n");
                                    } catch (Exception e) {
                                        sink.next("文件解析失败 \n");
                                    }
                                }
                                disposable[1] = streamingChatWithBaseKnowledge(message, topicId, fileContent)
                                        .doOnNext(sink::next)
                                        .doOnComplete(sink::complete)
                                        .subscribe();
                                break;
                            case "3":
                                sink.next("【系统】闲聊中...\n");
                                disposable[1] = streamingChat(message, topicId)
                                        .doOnNext(sink::next)
                                        .doOnComplete(sink::complete)
                                        .subscribe();
                                break;
                            default:
                                sink.next("意图识别失败");
                                sink.complete();
                                return;
                        }
                    }, error -> {
                        error.printStackTrace();
                        sink.next("系统异常：" + error.getMessage());
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

    private Flux<String> dbAgentLLMAnswer(List<Document> results, String sqlResult, String message, String chatId) {
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
        return ollamaChatModel.stream(new Prompt(answerPrompt.toString()))
                .map(chatResp -> chatResp.getResult().getOutput().getText())
                .doOnNext(chunk -> {
                    resultBuilder.append(chunk);
                }).doOnComplete(() -> {
                    String result = resultBuilder.toString();
                    var answer = extractAnswerOnly(result);
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
        Flux<ChatResponse> stream = ollamaChatModel.stream(new Prompt(promptBuilder.toString()));

        // 回答并缓存问答
        StringBuilder fullAnswerBuilder = new StringBuilder();
        return stream.map(chatResp -> chatResp.getResult().getOutput().getText())
                .doOnNext(chunk -> {
                    fullAnswerBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    // 在流完成后存储完整的问答
                    String fullAnswer = extractAnswerOnly(fullAnswerBuilder.toString());
                    if (!fullAnswer.isEmpty()) {
                        chatContextManager.putContextToCache(chatId, message, fullAnswer);
                    }
                }).doOnCancel(() -> {
                    System.out.println("回答取消");
                });
        // return ollamaChatModel.stream(new
        // Prompt(promptBuilder.toString())).map(resp->resp.getResult().getOutput().getText());
    }

    private Flux<String> streamingChatWithBaseKnowledge(String message, String chatId, List<String> fileContent) {
        List<String> context = chatContextManager.getAllContextFromCache(chatId);
        var vectorStore = vectorStoreFactory.createVectorStore("pro_knowledge", "pro_knowledge", embeddingModel);
        List<Document> searchResults = new ArrayList<>(vectorStore.similaritySearch(SearchRequest.builder()
                .query(message)
                .similarityThreshold(0.8f)
                .topK(18)
                .build()));

        System.out.println("knowledge size: " + searchResults.size());
        if (!fileContent.isEmpty()) {
            fileContent.forEach(c -> searchResults.add(new Document(c)));
        }

        var promptBuilder = new StringBuilder();
        promptBuilder.append(GlobalPrompt.IDENTITY_STRING);
        if (context.isEmpty()) {
            promptBuilder.append("##用户当前的问题是：\n").append(message).append("\n");
        } else {
            promptBuilder.append("##用户当前的问题是：\n").append(message).append("\n");
            promptBuilder.append("##你需要结合上下文作出合理、自然的回答\n");
            promptBuilder.append("##上下文如下：\n");
            context.forEach(promptBuilder::append);
            promptBuilder.append("##如果上下文内容与这次问题无关，忽略上下文\n");
        }
        if (!searchResults.isEmpty()) {
            promptBuilder.append("##你需要结合知识库作出合理、自然的回答\n");
            promptBuilder.append("##仅使用与问题相关的知识库内容，忽略无关内容\n");
            promptBuilder.append("##如果知识库内容无法完全回答，可以补充你自己的知识。\n");
            promptBuilder.append("##知识库如下：\n");
            for (Document doc : searchResults) {
                promptBuilder.append("##").append(doc.getText()).append("\n");
            }

        }

        Prompt prompt = new Prompt(promptBuilder.toString());
        Flux<ChatResponse> stream = ollamaChatModel.stream(prompt);

        // 回答并缓存问答
        StringBuilder fullAnswerBuilder = new StringBuilder();
        return stream.map(chatResp -> chatResp.getResult().getOutput().getText())
                .doOnNext(chunk -> {
                    fullAnswerBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    // 在流完成后存储完整的问答
                    String fullAnswer = extractAnswerOnly(fullAnswerBuilder.toString());
                    if (!fullAnswer.isEmpty()) {
                        chatContextManager.putContextToCache(chatId, message, fullAnswer);
                    }
                }).doOnCancel(() -> {
                    System.out.println("回答取消");
                });
    }

    private Mono<String> intentMsgAsync(String message, List<String> contexts) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("##你是一个智能意图识别助手。请根据用户的历史对话上下文和当前问题，判断其意图属于以下哪一类，只返回编号，不要解释：\n")
                .append(IntentsEnum.print());
        if (contexts != null && !contexts.isEmpty()) {
            prompt.append("##历史上下文如下：\n");
            // prompt.append(context).append("\n");
            contexts.forEach(c -> prompt.append(c).append("\n"));
        }
        prompt.append("##当前问题：").append(message).append("\n");

        Prompt promptWithModelChose = new Prompt(prompt.toString(), ChatOptions.builder()
                .model("qwen3:4b")
                .build());
        return Mono.fromCallable(() -> ollamaChatModel.call(promptWithModelChose).getResult().getOutput().getText())
                .subscribeOn(Schedulers.boundedElastic())
                .doOnCancel(() -> {
                    System.out.println("取消意图识别");
                })
                .map(this::extractAnswerOnly);
    }

    // SQL生成流式收集
    private Mono<String> sqlGenerateAsync(List<Document> results, String message, String topicId) {
        var topic = "sql-" + topicId;
        StringBuilder prompt = new StringBuilder();
        prompt.append("###你是SQL生成助手。请根据下方数据库结构描述、用户问题以及上轮回答, 直接生成对应的SQL语句, 只输出SQL, 不要解释。\n")
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
                .model("qwen3:4b")
                .build());
        return ollamaChatModel.stream(promptWithModelChose)
                .map(chatResp -> chatResp.getResult().getOutput().getText())
                .reduce(new StringBuilder(), StringBuilder::append)
                .map(StringBuilder::toString)
                .map(this::extractAnswerOnly)
                .doOnSuccess(sql -> {
                    if (StringUtils.isNotBlank(sql)) {
                        chatContextManager.putContextToCache(topic, message, sql);
                    }
                })
                .doOnError(error -> {

                    System.err.println("Error during SQL generation: " + error.getMessage());
                });
    }

    private String extractAnswerOnly(String text) {
        String[] lines = text.split("</think>");
        if (lines.length < 2)
            return "";
        return lines[1].trim();
    }

}
