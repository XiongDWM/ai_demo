package com.xiongdwm.ai_demo.utils.global;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionWork;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;

@Component
public class Neo4jIndexer {

    @Autowired
    private Driver driver;

    @Autowired
    private EmbeddingModel embeddingModel;

    /**
     * 将 SectionNode 列表写入 Neo4j：创建/更新 Section 节点并建立 HAS_CHILD 和 HAS_IMAGE 关系，同时写入 embedding 属性
     */
    public void indexSections(List<SectionNode> nodes) throws Exception {
        if (nodes == null || nodes.isEmpty()) return;
        // 1. 生成文本列表并调用 embeddingModel
        List<String> texts = new ArrayList<>();
        for (SectionNode n : nodes) {
            String t = (n.getTitle() == null ? "" : n.getTitle()) + "\n" + (n.getText() == null ? "" : n.getText());
            texts.add(t);
        }
        EmbeddingResponse resp = embeddingModel.embedForResponse(texts);
        // 假定每个结果为 float[] 或 double[]; spring.ai EmbeddingResponse 提供 getResults()
        List<double[]> embeddings = new ArrayList<>();
        resp.getResults().forEach(r -> {
            var arr = r.getOutput();
            double[] d = new double[arr.length];
            for (int i = 0; i < arr.length; i++) d[i] = arr[i];
            embeddings.add(d);
        });

        // 2. 写入 Neo4j
        try (Session session = driver.session()) {
            session.writeTransaction((TransactionWork<Void>) tx -> {
                for (int i = 0; i < nodes.size(); i++) {
                    SectionNode n = nodes.get(i);
                    double[] emb = embeddings.get(i);
                    List<Double> embList = new ArrayList<>();
                    for (double v : emb) embList.add(v);

                    Map<String, Object> params = new HashMap<>();
                    params.put("id", n.getId());
                    params.put("title", n.getTitle());
                    params.put("text", n.getText());
                    params.put("level", n.getLevel());
                    params.put("imageUrls", n.getImageUrls());
                    params.put("steps", n.getSteps());
                    params.put("embedding", embList);

                    String upsert = "MERGE (s:Section {id:$id}) SET s.title=$title, s.text=$text, s.level=$level, s.imageUrls=$imageUrls, s.steps=$steps, s.embedding=$embedding";
                    tx.run(upsert, params);

                    if (n.getParentId() != null && !n.getParentId().isEmpty()) {
                        tx.run("MATCH (p:Section {id:$parentId}), (s:Section {id:$id}) MERGE (p)-[:HAS_CHILD]->(s)",
                                Map.of("parentId", n.getParentId(), "id", n.getId()));
                    }

                    if (n.getImageUrls() != null) {
                        for (String url : n.getImageUrls()) {
                            tx.run("MERGE (img:Image {url:$url}) SET img.url=$url WITH img MATCH (s:Section {id:$id}) MERGE (s)-[:HAS_IMAGE]->(img)",
                                    Map.of("url", url, "id", n.getId()));
                        }
                    }
                }
                return null;
            });
        }
    }
}

