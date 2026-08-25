package com.xiongdwm.ai_demo.embedding.ingest.strategy;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class HybridSearchStrategy {
    private final Driver driver;

    public HybridSearchStrategy(Driver driver) {
        this.driver = driver;
    }

    public List<Document> hybridSearch(String queryText,float[] queryVector, String fulltextIndex,String vectorIndex,
                                       float confidenceText,float confidenceVector, int topK) {
        List<Document> results = new ArrayList<>();

        try (Session session = driver.session()) {
            String cypher = """
                CALL db.index.vector.queryNodes($vectorIndex, $topK, $vector)
                YIELD node AS vecNode, score AS vecScore
                
                OPTIONAL MATCH (ftNode)
                WHERE elementId(vecNode) = elementId(ftNode)
                CALL db.index.fulltext.queryNodes($fulltextIndex, $queryText, {limit: 1})
                YIELD node AS ftCheckNode, score AS ftScore
                WHERE elementId(ftCheckNode) = elementId(vecNode)
                
                WITH vecNode, vecScore, COALESCE(ftScore, 0) as finalFtScore
                ORDER BY (vecScore * $confidenceVector + finalFtScore * $confidenceText) DESC
                LIMIT $topK
                
                RETURN vecNode AS node, vecScore AS score
                """;

            var result = session.run(cypher,
                    Map.of("vectorIndex", vectorIndex,
                            "fulltextIndex", fulltextIndex,
                            "vector", queryVector, // 直接使用传入的向量
                            "queryText", queryText,
                            "confidenceVector", confidenceVector,
                            "confidenceText", confidenceText,
                            "topK", topK));

            result.forEachRemaining(record -> {
                var node = record.get("node").asNode();
                var score = record.get("score").asFloat();
                results.add(nodeToDocument(node, score));
            });

        } catch (Exception e) {
            throw new RuntimeException("混合检索执行失败", e);
        }

        return results;
    }

    private Document nodeToDocument(org.neo4j.driver.types.Node node, float score) {
        String text = node.get("text").asString();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("score", score);
        metadata.put("id", node.elementId());

        for (String key : node.keys()) {
            if (!key.equals("text") && !key.equals("embedding")) {
                metadata.put(key, node.get(key).asObject());
            }
        }

        return new Document(text, metadata);
    }
}
