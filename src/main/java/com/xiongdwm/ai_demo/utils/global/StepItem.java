package com.xiongdwm.ai_demo.utils.global;

import java.util.ArrayList;
import java.util.List;

public class StepItem {
    private int index;
    private String text = "";
    private List<String> imageUrls = new ArrayList<>();

    public StepItem(int index) { this.index = index; }

    public int getIndex() { return index; }
    public String getText() { return text; }
    public List<String> getImageUrls() { return imageUrls; }

    public void appendText(String more) {
        if (more == null || more.isEmpty()) return;
        if (this.text.isEmpty()) this.text = more.trim();
        else this.text = this.text + "\n" + more.trim();
    }

    public void addImage(String url) {
        if (url != null && !url.isEmpty()) imageUrls.add(url);
    }
}

