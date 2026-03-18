package com.xiongdwm.ai_demo.utils.global;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.xiongdwm.ai_demo.utils.excepotion.ServiceException;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFStyles;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.ai.document.Document;

public class WordSplitHelper {
    public record Chunk(
            String content,
            String file,
            double weight,
            int startPos,
            int endPos,
            int index

    ){
        public Document toDocument(){
            Map<String,Object> meta=Map.of("weight",weight,"index",index,"startPos",startPos,"endPos",endPos,"fileUnique",file);
            return new Document(content,meta);
        }

    }

    private static final Map<Integer,Double> MAP_SYMBOL=PunctuationWeightEnum.getSymbolToValueAsKeyInInteger();
    private static final double MAX_WEIGHT=14.0;
    private static final int MAX_CHUNK_SIZE=300;
    private static final int MIN_CHUNK_SIZE=80;

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

    public static String getAllText(String filePath) throws Exception {
        StringBuilder text = new StringBuilder();
        XWPFDocument doc = new XWPFDocument(new FileInputStream(filePath));
        for (IBodyElement element : doc.getBodyElements()) {
            if (element instanceof XWPFParagraph paragraph) {
                if(paragraph.getText().trim().isEmpty())continue;
                text.append(paragraph.getText().trim()).append("\n");
            }
            if(element instanceof XWPFTable table){
                text.append(formatTable(table)).append("\n");
            }
        }
        doc.close();
        if(text.isEmpty())throw new ServiceException("读取文档内容失败");
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
            if(element instanceof XWPFTable table){
                var formattedTable = formatTable(table).trim();
                rawParagraphs.add(formattedTable);
            }
        }
        doc.close();
        if(rawParagraphs.size()<=MAX_CHUNK_SIZE)return rawParagraphs;
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
        System.out.println(result);
        return result;
    }

    //（需要按照符号对于每个chunk设置一个权合，按照权合来拆分文档内容）
    public static List<Chunk> splitChunkByWeight(String filePath) throws Exception {
        List<Chunk> chunks = new ArrayList<>();
        var uuid=java.util.UUID.randomUUID().toString();
        String content = getAllText(filePath);
        if (content.isEmpty()) return chunks;

        StringBuilder sb = new StringBuilder();
        double weightSum = 0;
        int lastStrongEnd = -1;
        int lastWeakEnd = -1;
        int lastPuncEnd = -1;

        int globalStart = 0;
        int chunkIndex = 0;

        for (int idx = 0; idx < content.length(); idx++) {
            char ch = content.charAt(idx);
            sb.append(ch);

            double w = MAP_SYMBOL.getOrDefault((int) ch, 0d);
            weightSum += w;

            if ("。.!！?？".indexOf(ch) >= 0) {
                lastStrongEnd = sb.length();
                lastWeakEnd = -1;
                lastPuncEnd = lastStrongEnd;
            } else if ("，,、；;".indexOf(ch) >= 0) {
                lastWeakEnd = sb.length();
                lastPuncEnd = lastWeakEnd;
            } else if ("：:".indexOf(ch) >= 0) {
                lastPuncEnd = sb.length();
            }

            boolean reachMin = sb.length() >= MIN_CHUNK_SIZE;
            boolean reachMaxWeight = weightSum >= MAX_WEIGHT;
            if (reachMin && reachMaxWeight) {
                int splitPos = sb.length();
                boolean endsWithColon = sb.charAt(sb.length() - 1) == ':' || sb.charAt(sb.length() - 1) == '：';

                if (endsWithColon && lastPuncEnd > 0 && lastPuncEnd < sb.length()) {
                    splitPos = lastPuncEnd;
                } else if (lastStrongEnd > 0) {
                    splitPos = lastStrongEnd;
                } else if (lastWeakEnd > 0) {
                    splitPos = lastWeakEnd;
                }

                String head = sb.substring(0, splitPos).trim();
                if (!head.isEmpty()) {
                    int headEndGlobal = globalStart + splitPos;
                    chunks.add(new Chunk(head, uuid,weightSum, globalStart, headEndGlobal, chunkIndex++));
                    globalStart = headEndGlobal;
                }

                String tail = sb.substring(splitPos);
                sb.setLength(0);
                sb.append(tail);

                weightSum = 0;
                lastStrongEnd = -1;
                lastWeakEnd = -1;
                lastPuncEnd = -1;
                for (int j = 0; j < sb.length(); j++) {
                    char c = sb.charAt(j);
                    weightSum += MAP_SYMBOL.getOrDefault((int) c, 0d);
                    if ("。.!！?？".indexOf(c) >= 0) {
                        lastStrongEnd = j + 1;
                        lastWeakEnd = -1;
                        lastPuncEnd = lastStrongEnd;
                    } else if ("，,、；;".indexOf(c) >= 0) {
                        lastWeakEnd = j + 1;
                        lastPuncEnd = lastWeakEnd;
                    } else if ("：:".indexOf(c) >= 0) {
                        lastPuncEnd = j + 1;
                    }
                }
            }
        }

        if (!sb.isEmpty()) {
            String tail = sb.toString().trim();
            if (!tail.isEmpty()) {
                int endGlobal = globalStart + sb.length();
                chunks.add(new Chunk(tail,uuid,weightSum, globalStart, endGlobal, chunkIndex));
            }
        }
        return chunks;
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
