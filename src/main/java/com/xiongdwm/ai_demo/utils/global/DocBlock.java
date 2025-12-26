package com.xiongdwm.ai_demo.utils.global;

import java.util.Objects;

public class DocBlock {
    public enum Type { TEXT, IMAGE }

    private final Type type;
    private final String text;
    private final String imageUrl;

    public DocBlock(Type type, String text, String imageUrl) {
        this.type = Objects.requireNonNull(type);
        this.text = text;
        this.imageUrl = imageUrl;
    }

    public static DocBlock text(String text) {
        return new DocBlock(Type.TEXT, text, null);
    }

    public static DocBlock image(String url) {
        return new DocBlock(Type.IMAGE, null, url);
    }

    public Type getType() { return type; }
    public String getText() { return text; }
    public String getImageUrl() { return imageUrl; }
}

