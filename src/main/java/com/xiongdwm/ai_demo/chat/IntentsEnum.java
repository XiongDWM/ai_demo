package com.xiongdwm.ai_demo.chat;

public enum IntentsEnum {
    DB_QUERY(1, "数据库查询"),
    KB_QUERY(2, "知识库问答"),
    CASUAL_CHAT(3, "闲聊");
    private int code;
    private String name;

    IntentsEnum(int code, String name) {
        this.code = code;
        this.name = name;
    }
    public int getCode() {
        return code;
    }
    public String getName() {
        return name;
    }
    public static String print(){
        StringBuilder sb = new StringBuilder();
        for (IntentsEnum intentsEnum : IntentsEnum.values()) {
            sb.append(intentsEnum.getCode()).append(". ").append(intentsEnum.getName()).append("\n");
        }
        return sb.toString();
    }
}
