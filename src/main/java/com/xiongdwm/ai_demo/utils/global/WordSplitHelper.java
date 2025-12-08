package com.xiongdwm.ai_demo.utils.global;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.XWPFTable;

public class WordSplitHelper {

    private final Map<Integer,Double> MAP_SYMBOL=PunctuationWeightEnum.getSymbolToValueAsKeyInInteger();
    private final double MAS_WEIGHT=14.0;
    private final int MAX_CHUNK_SIZE=300;
    private final int MIN_CHUNK_SIZE=80;

    public record SplitCandidate(
            double weight,
            int pos
    ){

    }


    public static List<String> splitByHeadings(String filePath) throws FileNotFoundException, IOException {
        var chunks = new ArrayList<String>();
        XWPFDocument doc = new XWPFDocument(new FileInputStream(filePath));
        XWPFStyles styles = doc.getStyles();
        StringBuilder chunkBuilder = new StringBuilder();
        for (IBodyElement element : doc.getBodyElements()) {
            if (element instanceof XWPFParagraph paragraph) {
                String styleName = paragraph.getStyle();
                if(styleName!=null)styleName=styles.getStyle(styleName).getName().toLowerCase();
                if (styleName != null && styleName.startsWith("heading")) {
                    if (!chunkBuilder.isEmpty()) {
                        chunks.add(chunkBuilder.toString());
                        chunkBuilder.setLength(0);
                    }
                    chunkBuilder.append("## ").append(paragraph.getText()).append("\n");
                } else {
                    chunkBuilder.append(paragraph.getText()).append("\n");
                }
            }
        }
        if (!chunkBuilder.isEmpty()) {
            chunks.add(chunkBuilder.toString());
        }
        doc.close();
        return chunks;
    }

    public String getAllText(String filePath) throws Exception {
        StringBuilder text = new StringBuilder();
        XWPFDocument doc = new XWPFDocument(new FileInputStream(filePath));
        for (IBodyElement element : doc.getBodyElements()) {
            if (element instanceof XWPFParagraph paragraph) {
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
            if (element instanceof XWPFParagraph paragraph) {
                String text = paragraph.getText().trim();
                if (!text.isEmpty()) {
                    rawParagraphs.add(text);
                }
            }
        }
        doc.close();

        StringBuilder currentBlock = new StringBuilder();
        boolean inBlock = false;
        for (String line : rawParagraphs) {
            if (line.endsWith(":") || line.endsWith("：")) {
                if (!currentBlock.isEmpty()) {
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
        if (!currentBlock.isEmpty()) {
            result.add(currentBlock.toString().trim());
        }
        return result;
    }

    //（需要按照符号对于每个chunk设置一个权合，按照权合来拆分文档内容）
    public List<String>splitChunkByWeight(String filePath){
        final int a=10;
        // 应该是先byheadings，然后byParagraph，然后每段落再通过计算权值分成chunk。
        // 拆分方式，获得这个candidatelist后逐个计算权值，然后达到阈值时查看是否为逗号（先做简单的，只看逗号或者句号），如果是，向前后找到句号，
        // 与此同时，需要每次检查chunk是否超过max-chunk-size 或者低于min-chunk-size 如果是则需要向前减少或者向后补
        // 另外循环外维护一个int，记录上一个句号在candidatelist中的下标，可以用的地方是向前缩进
        // 各个chunk之间的overlap，

        return new ArrayList<>();
    }
    public List<SplitCandidate> getSplitCandidateList(String context){
        List<SplitCandidate> candidates=new ArrayList<>();
        AtomicInteger i= new AtomicInteger();
        context.chars().forEach(it->{
            i.getAndIncrement();
            double v=MAP_SYMBOL.getOrDefault(it,-1.0d);
            if(v<0)return;
            candidates.add(new SplitCandidate(v,i.get()));
        });

        return candidates;
    }
    private static String formatTable(XWPFTable table) {
        StringBuilder sb = new StringBuilder();
            sb.append("[表格]\n");
            for (var row : table.getRows()) {
                List<String> cells = new ArrayList<>();
                row.getTableCells().forEach(cell -> cells.add(cell.getText().replace("\n", " ")));
                sb.append(String.join(" | ", cells)).append("\n");
            }
            sb.append("[/表格]");
            return sb.toString();
    }

    public static void main(String[] args) {
        String s="你好啊，今天很冷，我的撒打发，你说的发围绕大大, sdfa. dfaaea!dfafafa！。";
        char douhaos = '，';
        char commas=',';
        System.out.println((int)douhaos+","+(int)commas);
        System.out.println(s.indexOf("，"));
        final int last=s.lastIndexOf("，");
        System.out.println(last);
        int next=s.indexOf("，",3+1,last);
        System.out.println(next);
        System.out.println("==========================");
        WordSplitHelper helper=new WordSplitHelper();
        System.out.println(helper.getSplitCandidateList(s));
    }
}
