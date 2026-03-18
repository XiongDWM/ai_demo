package com.xiongdwm.ai_demo.embedding.ingest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongdwm.ai_demo.utils.global.SectionNode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionWork;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CustomerServiceGraphService {

    private final Driver driver;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public CustomerServiceGraphService(Driver driver) {
        this.driver = driver;
    }

    /**
     * 根据 SectionNode 列表构建知识图谱：Manual/Section/Step/Image 节点 + HAS_CHILD/HAS_STEP/HAS_PIC 关系。
     * 不做图片 caption；步骤来自 SectionNode.steps 的 JSON（index/text/imageUrls）。
     */
    public void buildGraph(List<SectionNode> nodes, String filePath) {
        if (nodes == null || nodes.isEmpty()) return;
        try (Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                // 根手册节点（按文件路径聚合）
                tx.run("MERGE (m:Manual {filePath:$filePath}) SET m.filePath=$filePath",
                        Map.of("filePath", filePath));

                for (SectionNode n : nodes) {
                    Map<String, Object> params = new HashMap<>();
                    params.put("id", n.getId());
                    params.put("title", n.getTitle());
                    params.put("text", n.getText());
                    params.put("level", n.getLevel());
                    params.put("filePath", filePath);

                    tx.run("MERGE (s:Section {id:$id}) SET s.title=$title, s.text=$text, s.level=$level, s.filePath=$filePath",
                            params);

                    // 关联到手册
                    tx.run("MATCH (m:Manual {filePath:$filePath}), (s:Section {id:$id}) MERGE (m)-[:HAS_CHILD]->(s)",
                            Map.of("filePath", filePath, "id", n.getId()));

                    // 父子关系
                    if (n.getParentId() != null && !n.getParentId().isEmpty()) {
                        tx.run("MATCH (p:Section {id:$pid}),(s:Section {id:$id}) MERGE (p)-[:HAS_CHILD]->(s)",
                                Map.of("pid", n.getParentId(), "id", n.getId()));
                    }

                    // Section 级图片（如果不想重复，也可以只挂在 Step 上）
                    if (n.getImageUrls() != null) {
                        for (String url : n.getImageUrls()) {
                            tx.run("MERGE (img:Image {url:$url}) WITH img MATCH (s:Section {id:$id}) MERGE (s)-[:HAS_IMAGE]->(img)",
                                    Map.of("url", url, "id", n.getId()));
                        }
                    }

                    // 步骤与 NEXT_STEP 串联
                    Integer prevIdx = null;
                    if (n.getSteps() != null) {
                        for (String stepJson : n.getSteps()) {
                            try {
                                Map<String, Object> step = MAPPER.readValue(stepJson, new TypeReference<Map<String, Object>>(){});
                                int idx = (step.get("index") instanceof Number) ? ((Number) step.get("index")).intValue() : 0;
                                String text = Objects.toString(step.get("text"), "");
                                @SuppressWarnings("unchecked")
                                List<String> imgs = (List<String>) step.getOrDefault("imageUrls", Collections.emptyList());

                                Map<String, Object> p2 = new HashMap<>();
                                p2.put("sid", n.getId());
                                p2.put("idx", idx);
                                p2.put("text", text);

                                tx.run("MERGE (st:Step {sectionId:$sid, index:$idx}) SET st.text=$text",
                                        p2);
                                tx.run("MATCH (s:Section {id:$sid}),(st:Step {sectionId:$sid, index:$idx}) MERGE (s)-[:HAS_STEP]->(st)",
                                        p2);

                                if (prevIdx != null && idx != prevIdx) {
                                    tx.run("MATCH (a:Step {sectionId:$sid, index:$a}), (b:Step {sectionId:$sid, index:$b}) MERGE (a)-[:NEXT_STEP]->(b)",
                                            Map.of("sid", n.getId(), "a", prevIdx, "b", idx));
                                }
                                prevIdx = idx;

                                for (String url : imgs) {
                                    tx.run("MERGE (img:Image {url:$url}) WITH img MATCH (st:Step {sectionId:$sid, index:$idx}) MERGE (st)-[:HAS_PIC]->(img)",
                                            Map.of("url", url, "sid", n.getId(), "idx", idx));
                                }
                            } catch (Exception ignore) { }
                        }
                    }
                }
                return null;
            });
        }
    }

    /**
     * 根据 sectionId 列表，查询每个 section 在手册中的路径（手册+各级标题）。
     * 返回列表项包含：sectionId, manualFilePath, sectionPath(标题数组)
     */
    public List<Map<String, Object>> getSectionPaths(List<String> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) return Collections.emptyList();
        String cypher = "MATCH (s:Section) WHERE s.id IN $ids " +
                "OPTIONAL MATCH p = (m:Manual)-[:HAS_CHILD*]->(s) " +
                "RETURN s.id AS sectionId, m.filePath AS manual, " +
                "CASE WHEN p IS NULL THEN [] ELSE [x IN nodes(p) WHERE x:Section | x.title] END AS titles";
        try (Session session = driver.session()) {
            return session.readTransaction(tx -> {
                var result = tx.run(cypher, Map.of("ids", sectionIds));
                return result.list(r -> {
                    String sid = r.get("sectionId").isNull() ? "" : r.get("sectionId").asString("");
                    String manual = r.get("manual").isNull() ? "" : r.get("manual").asString("");
                    List<String> titles = toStringList(r.get("titles"));
                    Map<String, Object> m = new HashMap<>();
                    m.put("sectionId", sid);
                    m.put("manualFilePath", manual);
                    m.put("sectionPath", titles);
                    return m;
                });
            });
        }
    }

    private static List<String> toStringList(Value v) {
        if (v == null || v.isNull()) return Collections.emptyList();
        List<String> list = new ArrayList<>();
        for (var x : v.asList()) list.add(String.valueOf(x));
        return list;
    }
}
