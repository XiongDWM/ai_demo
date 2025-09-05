package com.xiongdwm.ai_demo.utils.config.langchain4j;


import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.ai.ollama")
public class OllamaProperties {
    private String baseUrl;
    private String chatOptionsModel;
    private Integer timeout;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getChatOptionsModel() {
        return chatOptionsModel;
    }

    public void setChatOptionsModel(String chatOptionsModel) {
        this.chatOptionsModel = chatOptionsModel;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

}
