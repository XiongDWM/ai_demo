package com.xiongdwm.ai_demo.utils.global;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFStyles;

public class WordSplitHelper {
    public static List<String> splitByHeadings(String filePath) throws FileNotFoundException, IOException {
        var chunks = new ArrayList<String>();
        XWPFDocument doc = new XWPFDocument(new FileInputStream(filePath));
        XWPFStyles styles = doc.getStyles();
        StringBuilder chunkBuilder = new StringBuilder();
        for (IBodyElement element : doc.getBodyElements()) {
            if (element instanceof XWPFParagraph) {
                XWPFParagraph paragraph = (XWPFParagraph) element;
                String styleName = paragraph.getStyle();
                if(styleName!=null)styleName=styles.getStyle(styleName).getName().toLowerCase();
                if (styleName != null && styleName.startsWith("heading")) {
                    if (chunkBuilder.length() > 0) {
                        chunks.add(chunkBuilder.toString());
                        chunkBuilder.setLength(0);
                    }
                    chunkBuilder.append("## ").append(paragraph.getText()).append("\n");
                } else {
                    chunkBuilder.append(paragraph.getText()).append("\n");
                }
            }
        }
        if (chunkBuilder.length() > 0) {
            chunks.add(chunkBuilder.toString());
        }
        doc.close();
        return chunks;
    }

    public String getAllText(String filePath) throws Exception {
        StringBuilder text = new StringBuilder();
        XWPFDocument doc = new XWPFDocument(new FileInputStream(filePath));
        for (IBodyElement element : doc.getBodyElements()) {
            if (element instanceof XWPFParagraph) {
                XWPFParagraph paragraph = (XWPFParagraph) element;
                text.append(paragraph.getText()).append("\n");
            }
        }
        doc.close();
        return text.toString();
    }

    public static List<String> splitByParagraphs(String filePath) throws Exception {
        List<String> result = new ArrayList<>();
        XWPFDocument doc = new XWPFDocument(new FileInputStream(filePath));
        List<String> rawParagraphs = new ArrayList<>();
        for (IBodyElement element : doc.getBodyElements()) {
            if (element instanceof XWPFParagraph) {
                XWPFParagraph paragraph = (XWPFParagraph) element;
                String text = paragraph.getText().trim();
                if (!text.isEmpty()) {
                    rawParagraphs.add(text);
                }
            }
        }
        doc.close();

        StringBuilder currentBlock = new StringBuilder();
        boolean inBlock = false;
        for (int i = 0; i < rawParagraphs.size(); i++) {
            String line = rawParagraphs.get(i);
            if (line.endsWith(":") || line.endsWith("：")) {
                if (currentBlock.length() > 0) {
                    result.add(currentBlock.toString().trim());
                    currentBlock.setLength(0);
                }
                currentBlock.append(line).append("\n");
                inBlock = true;
            } else if (inBlock && (line.matches("^\\d+\\.\\s*.*") || line.matches("^\\d+、.*"))) {
                currentBlock.append(line).append("\n");
            } else if (inBlock) {
                currentBlock.append(line).append("\n");
            } else {
                result.add(line);
            }
        }
        if (currentBlock.length() > 0) {
            result.add(currentBlock.toString().trim());
        }
        return result;
    }
}
