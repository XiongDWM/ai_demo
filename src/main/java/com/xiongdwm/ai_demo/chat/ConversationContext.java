package com.xiongdwm.ai_demo.chat;

public class ConversationContext {
    private String conversationId;
    private String content;

    public ConversationContext() {
    }
    public ConversationContext(String content, String conversationId) {
        this.conversationId = conversationId;
        this.content = content;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
    
}
