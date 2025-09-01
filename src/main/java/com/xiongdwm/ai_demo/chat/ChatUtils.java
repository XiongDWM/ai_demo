package com.xiongdwm.ai_demo.chat;

public class ChatUtils {
    
    public static String extractAnswerOnly(String text) {
        String[] lines = text.split("</think>");
        if (lines.length < 2)
            return "";
        return lines[1].trim();
    }

    public static String extractThoughtOnly(String text){
        String[] lines = text.split("</think>");
        if (lines.length < 2)
            return "";
        return lines[0].trim();
    }
}
