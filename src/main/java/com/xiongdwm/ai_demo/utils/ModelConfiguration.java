package com.xiongdwm.ai_demo.utils;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ModelConfiguration {

//    @Bean("dashscopeEmbedding")
//    public EmbeddingModel dashscopeEmbeddingModel(DashScopeEmbeddingModel dashScopeEmbeddingModel) {
//        return dashScopeEmbeddingModel;
//    }

    @Primary
    @Bean("ollamaEmbedding")
    public EmbeddingModel ollamaEmbeddingModel (OllamaEmbeddingModel ollamaEmbeddingModel) {
        return ollamaEmbeddingModel;
    }

    @Bean("dashscopeChat")
    public ChatModel dashscopeChatModel(DashScopeChatModel dashScopeChatModel) {
        return dashScopeChatModel;
    }

    @Bean("ollamaChat")
    public ChatModel ollamaChatModel (OllamaChatModel ollamaChatModel) {
        return ollamaChatModel;
    }

}
