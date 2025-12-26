package com.xiongdwm.ai_demo.utils.global;

import com.xiongdwm.ai_demo.ingest.ImageCaptionClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HierarchicalWordSplitHelper {

    @Value("${pic.doc.path.cs}")
    private String docPicPath;

    private static final Pattern NUMBERING_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)*)\\s+(.+)$");
    private static final Pattern STEP_PATTERN = Pattern.compile("^(步骤|Step)\\s*\\d+[:\\.\\s].*$", Pattern.CASE_INSENSITIVE);

    // 规范化段内文本，去除 URL 被换行或空格拆断的问题（http://、https://、IP、端口、点号等）
    private static String normalizeInlineArtifacts(String text) {
        if (text == null || text.isEmpty()) return text;
        String t = text;
        // 合并协议后的空白
        t = t.replaceAll("(?i)(https?://)\\s+", "$1");
        // 合并 www. 后的空白
        t = t.replaceAll("(?i)(www\\.)\\s+", "$1");
        // 合并 : 或 . 与数字之间的空白（: 880 -> :880, . 127 -> .127）
        t = t.replaceAll("([:\\.])\\s+(\\d)", "$1$2");
        // 合并数字后紧跟 : 或 . 中间的空白（127 . 0 -> 127.0）——修正为不对 '.' 进行多余转义
        t = t.replaceAll("(\\d)\\s+([:.])", "$1$2");
        // 如果包含 http/https，再把数字间断行也合并（880 0 -> 8800）
        if (t.toLowerCase().contains("http")) {
            t = t.replaceAll("(\\d)\\s+(\\d)", "$1$2");
        }
        return t;
    }

    // 注意：此方法为实例方法，调用方应注入 HierarchicalWordSplitHelper bean
    public List<SectionNode> parseHierarchy(String filePath, String imageSaveDir,
                                            ImageCaptionClient captionClient, int maxCharsPerChunk) throws Exception {
        Files.createDirectories(new File(imageSaveDir).toPath());
        List<SectionNode> nodes = new ArrayList<>();
        Deque<SectionNode> stack = new ArrayDeque<>();
        Map<String, List<DocBlock>> blocksBySection = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream is = Files.newInputStream(new File(filePath).toPath());
             XWPFDocument doc = new XWPFDocument(is)) {

            for (IBodyElement be : doc.getBodyElements()) {
                if (be instanceof XWPFParagraph para) {
                    String style = para.getStyle();
                    String paraText = para.getText() == null ? "" : para.getText().trim();

                    int level = detectHeadingLevel(style, paraText);
                    if (level > 0) {
                        String title = extractTitle(style, paraText);
                        String id = UUID.randomUUID().toString();
                        while (!stack.isEmpty() && stack.peek().getLevel() >= level) stack.pop();
                        String parentId = stack.isEmpty() ? null : stack.peek().getId();
                        SectionNode node = new SectionNode(id, title, level, parentId);
                        nodes.add(node);
                        // 初始化 block 列表
                        blocksBySection.put(node.getId(), new ArrayList<>());
                        stack.push(node);
                        continue;
                    }

                    if (STEP_PATTERN.matcher(paraText).find()) {
                        SectionNode cur = getOrCreateCurrent(stack, nodes);
                        var bl = getOrCreateBlockList(blocksBySection, cur.getId());
                        bl.add(DocBlock.text(paraText));
                        // 保留老逻辑
                        cur.addStep(paraText);
                        continue;
                    }

                    List<XWPFRun> runs = para.getRuns();
                    if (runs != null && !runs.isEmpty()) {
                        SectionNode cur = getOrCreateCurrent(stack, nodes);
                        var bl = getOrCreateBlockList(blocksBySection, cur.getId());
                        StringBuilder paragraphBuffer = new StringBuilder();
                        for (XWPFRun run : runs) {
                            String runText = run.text();
                            if (StringUtils.hasText(runText)) {
                                // 累积本段落的文字，不引入额外换行，避免拆断 URL
                                paragraphBuffer.append(runText);
                            }

                            List<XWPFPicture> pics = run.getEmbeddedPictures();
                            if (pics != null && !pics.isEmpty()) {
                                // 在图片出现前，先把累积的段落文字冲刷为一个 DocBlock
                                if (paragraphBuffer.length() > 0) {
                                    String ptxt = normalizeInlineArtifacts(paragraphBuffer.toString().trim());
                                    if (!ptxt.isEmpty()) {
                                        bl.add(DocBlock.text(ptxt));
                                        if (ptxt.length() > maxCharsPerChunk) {
                                            int pos = 0;
                                            while (pos < ptxt.length()) {
                                                int end = Math.min(ptxt.length(), pos + maxCharsPerChunk);
                                                cur.appendText(ptxt.substring(pos, end));
                                                pos = end;
                                            }
                                        } else {
                                            cur.appendText(ptxt);
                                        }
                                    }
                                    paragraphBuffer.setLength(0);
                                }
                                // 处理本 run 的图片
                                for (XWPFPicture pic : pics) {
                                    var picData = pic.getPictureData();
                                    if (picData == null) continue;
                                    String ext = picData.suggestFileExtension();
                                    byte[] data = picData.getData();
                                    String fileName = String.format("%s_%d.%s", UUID.randomUUID(), Instant.now().toEpochMilli(), ext);
                                    File out = new File(imageSaveDir, fileName);
                                    try (FileOutputStream fos = new FileOutputStream(out)) { fos.write(data); }
                                    // URL-style 拼接，避免 Windows 反斜杠；docPicPath 未配置则用 imageSaveDir
                                    String base = (this.docPicPath != null && !this.docPicPath.isEmpty()) ? this.docPicPath : imageSaveDir;
                                    String normalizedBase = base.replace("\\", "/");
                                    if (!normalizedBase.endsWith("/")) normalizedBase += "/";
                                    String imageUrl = normalizedBase + out.getName();
                                    cur.addImageUrl(imageUrl);
                                    bl.add(DocBlock.image(imageUrl));
                                }
                            }
                        }
                        // flush 残余段落文字
                        if (paragraphBuffer.length() > 0) {
                            String ptxt = normalizeInlineArtifacts(paragraphBuffer.toString().trim());
                            if (!ptxt.isEmpty()) {
                                bl.add(DocBlock.text(ptxt));
                                if (ptxt.length() > maxCharsPerChunk) {
                                    int pos = 0;
                                    while (pos < ptxt.length()) {
                                        int end = Math.min(ptxt.length(), pos + maxCharsPerChunk);
                                        cur.appendText(ptxt.substring(pos, end));
                                        pos = end;
                                    }
                                } else {
                                    cur.appendText(ptxt);
                                }
                            }
                        }
                    } else {
                        if (StringUtils.hasText(paraText)) {
                            SectionNode cur = getOrCreateCurrent(stack, nodes);
                            var bl = getOrCreateBlockList(blocksBySection, cur.getId());
                            String norm = normalizeInlineArtifacts(paraText);
                            bl.add(DocBlock.text(norm));
                            cur.appendText(norm);
                        }
                    }
                } else if (be instanceof XWPFTable table) {
                    StringBuilder tb = new StringBuilder();
                    table.getRows().forEach(row -> row.getTableCells().forEach(cell -> tb.append(cell.getText()).append("\n")));
                    if (!tb.isEmpty()) {
                        SectionNode cur = getOrCreateCurrent(stack, nodes);
                        var bl = getOrCreateBlockList(blocksBySection, cur.getId());
                        bl.add(DocBlock.text("[表格]\n" + tb));
                        cur.appendText("[表格]\n" + tb);
                    }
                }
            }
        }

        // 构建 id -> node 索引，便于查找父链
        Map<String, SectionNode> byId = new HashMap<>();
        for (SectionNode n : nodes) byId.put(n.getId(), n);

        // 过滤与分组合并为一轮：只对 text 非空的节点分组，并重写 parentId 为最近的非空祖先
        List<SectionNode> filtered = new ArrayList<>();
        for (SectionNode node : nodes) {
            if (!StringUtils.hasText(node.getText())) {
                continue; // 跳过空文本 section
            }
            // 计算最近的非空祖先作为 parentId
            String pid = node.getParentId();
            String effectiveParent = null;
            while (pid != null) {
                SectionNode p = byId.get(pid);
                if (p == null) break;
                if (StringUtils.hasText(p.getText())) { // 父节点文本非空，保留
                    effectiveParent = pid;
                    break;
                }
                pid = p.getParentId();
            }
            node.setParentId(effectiveParent);

            // 分组 steps 并序列化
            List<DocBlock> bl = blocksBySection.get(node.getId());
            if (bl != null && !bl.isEmpty()) {
                List<StepItem> steps = StepGrouper.groupBlocksToSteps(bl);
                for (StepItem s : steps) {
                    try {
                        String json = mapper.writeValueAsString(Map.of(
                                "index", s.getIndex(),
                                "text", s.getText(),
                                "imageUrls", s.getImageUrls()
                        ));
                        node.addStep(json);
                    } catch (Exception e) {
                        node.addStep("step-" + s.getIndex());
                    }
                }
            }

            filtered.add(node);
        }

        return filtered;
    }

    private static List<DocBlock> getOrCreateBlockList(Map<String, List<DocBlock>> map, String sectionId) {
        return map.computeIfAbsent(sectionId, k -> new ArrayList<>());
    }

    private static SectionNode getOrCreateCurrent(Deque<SectionNode> stack, List<SectionNode> nodes) {
        if (!stack.isEmpty()) return stack.peek();
        SectionNode root = new SectionNode(UUID.randomUUID().toString(), "ROOT", 1, null);
        nodes.add(root);
        stack.push(root);
        return root;
    }

    private static int detectHeadingLevel(String style, String text) {
        if (style != null) {
            String s = style.toLowerCase();
            if (s.contains("heading2") || s.contains("标题2") || s.contains("h2")||s.contains("2")) return 1;
            if (s.contains("heading3") || s.contains("标题3") || s.contains("h3")||s.contains("3")) return 2;
            if (s.contains("heading4") || s.contains("标题4") || s.contains("h4")||s.contains("4")) return 3;
            if (s.contains("heading5") || s.contains("标题5") || s.contains("h5")||s.contains("5")) return 4;
            if (s.contains("heading6") || s.contains("标题6") || s.contains("h6")||s.contains("6")) return 5;
            if (s.contains("heading7") || s.contains("标题7") || s.contains("h7")||s.contains("7")) return 6;
            if (s.contains("heading8") || s.contains("标题8") || s.contains("h8")||s.contains("8")) return 7;
            if (s.contains("heading9") || s.contains("标题9") || s.contains("h9")||s.contains("9")) return 8;
        }
        Matcher m = NUMBERING_PATTERN.matcher(text);
        if (m.find()) {
            String num = m.group(1);
            int dots = num.length() - num.replace(".", "").length();
            return dots + 1;
        }
        return 0;
    }

    private static String extractTitle(String style, String text) {
        Matcher m = NUMBERING_PATTERN.matcher(text);
        if (m.find()) return m.group(2).trim();
        return StringUtils.hasText(text) ? text : "Untitled";
    }

    public static void main(String[] args) {
        HierarchicalWordSplitHelper helper = new HierarchicalWordSplitHelper();
        try {
            List<SectionNode> nodes = helper.parseHierarchy("D:\\test_docs\\hc.docx",
                    "D:\\test_docs\\images", null, 1000);
            for (SectionNode n : nodes) {
                System.out.println(n);
                System.out.println("---------------------");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
