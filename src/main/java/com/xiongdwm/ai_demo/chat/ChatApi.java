package com.xiongdwm.ai_demo.chat;



import java.util.List;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
// import org.springframework.http.MediaType;
// import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
// import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.xiongdwm.ai_demo.utils.config.Neo4jVectorStoreFactory;

import reactor.core.publisher.Flux;

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

    @PostMapping(value="/streaming/chat", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Flux<String> streamingChat(@RequestParam("message")String message, @RequestHeader(value="chat-id",required=false) String chatId){
        if(message == null || message.isEmpty()){
            return Flux.just("消息不能为空");
        }
        if(chatId == null || chatId.isEmpty()){
            return Flux.just("chat-id不能为空");
        }
        List<String> context = chatContextManager.getAllContextFromCache(chatId);
        chatContextManager.print();

        // 构造 prompt
        var promptBuilder = new StringBuilder();
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
                });
        // return ollamaChatModel.stream(new Prompt(promptBuilder.toString())).map(resp->resp.getResult().getOutput().getText());   
    }

    @PostMapping("/streaming/chat/baseKnowledge")
    public Flux<String> streamingChatWithBaseKnowledge(@RequestParam("message")String message, @RequestHeader(value="chat-id",required=false) String chatId){
        if(message == null || message.isEmpty()){
            return Flux.just("消息不能为空");
        }
        if(chatId == null || chatId.isEmpty()){
            return Flux.just("chat-id不能为空");
        }
        List<String> context = chatContextManager.getAllContextFromCache(chatId);
        chatContextManager.print();

        var vectorStore = vectorStoreFactory.createVectorStore("base_knowledge", "base_knowledge", embeddingModel);
        List<Document> searchResults = vectorStore.similaritySearch(SearchRequest.builder()
            .query(message)
            .topK(5) // 搜索前 5 个相关文档
            .build());

        var promptBuilder = new StringBuilder();
        if (context.isEmpty()) {
            promptBuilder.append("##用户当前的问题是：\n").append(message).append("\n");
        } else {
            promptBuilder.append("##用户当前的问题是：\n").append(message).append("\n");
            promptBuilder.append("##你需要结合上下文作出合理的回答\n");
            promptBuilder.append("##上下文如下：\n");
            context.forEach(promptBuilder::append);
        }
        promptBuilder.append("##你需要结合知识库作出合理的回答\n");
        promptBuilder.append("##知识库如下：\n");
        for (Document doc : searchResults) {
            promptBuilder.append("##").append(doc.getText()).append("\n");
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
                });
    }

    private String extractAnswerOnly(String text){
        String[] lines = text.split("</think>");
        if(lines.length < 2) return "";
        return lines[1].trim();
    }

}
