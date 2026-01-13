package com.xiongdwm.ai_demo.chat;

public class ConversationContext {
    private String conversationId;
    private String content;
    private MessageType messageType=MessageType.TEXT;

    public enum MessageType {
        TEXT,
        PICTURE_URL,
        URL
    }

    public ConversationContext() {
    }
    public ConversationContext(String content, String conversationId) {
        this.conversationId = conversationId;
        this.content = content;
    }

    public ConversationContext(String conversationId, String content, MessageType messageType) {
        this.conversationId = conversationId;
        this.content = content;
        this.messageType = messageType;
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

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public static String getEmptyContextJsonString(){
        return "{\"conversationId\":\"\",\"content\":\"\",\"messageType\":\"TEXT\"}";
    }

    public static ConversationContext getEmptyContext(){
        return new ConversationContext();
    }
}
