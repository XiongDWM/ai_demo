package com.xiongdwm.ai_demo.utils.global;

import java.util.ArrayList;
import java.util.List;

public class SectionNode {
    private String id;
    private String title;
    private int level; // 1 = 大标题, 2 = 子标题 等
    private String parentId;
    private String text = "";
    private final List<String> imageUrls = new ArrayList<>();
    private final List<String> steps = new ArrayList<>();

    public SectionNode() {}

    public SectionNode(String id, String title, int level, String parentId) {
        this.id = id;
        this.title = title;
        this.level = level;
        this.parentId = parentId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public void appendText(String more) {
        if (this.text == null || this.text.isEmpty()) this.text = more == null ? "" : more;
        else if (more != null && !more.isEmpty()) this.text = this.text + "\n" + more;
    }
    public List<String> getImageUrls() { return imageUrls; }
    public void addImageUrl(String url) { if (url != null) this.imageUrls.add(url); }
    public List<String> getSteps() { return steps; }
    public void addStep(String step) { if (step != null) this.steps.add(step); }

    @Override
    public String toString() {
        return "SectionNode{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", level=" + level +
                ", parentId='" + parentId + '\'' +
                ", text='" + text + '\'' +
                ", imageUrls=" + imageUrls +
                ", steps=" + steps +
                '}';
    }
}

