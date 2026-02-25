package com.xiongdwm.ai_demo.ingest;

import com.xiongdwm.ai_demo.utils.config.Neo4jVectorStoreFactory;
import com.xiongdwm.ai_demo.utils.excepotion.ServiceException;
import com.xiongdwm.ai_demo.utils.global.ExcelParser;
import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class EmbeddingService {

    @Autowired
    @Qualifier("ollamaEmbedding")
    private EmbeddingModel embeddingModel;
    @Autowired
    private Neo4jVectorStoreFactory vectorStoreFactory;
    @Resource
    private CustomerServiceGraphService customerGraphService;

    public boolean createIndex(String index, String label, int dimension, String property, String similarity) {
        try {
            vectorStoreFactory.createVectorIndex(index, label, dimension, property, similarity);
        } catch (ServiceException e) {
            return false;
        }
        return true;
    }

    public VectorStore vectorStore(String label, String index) {
        return vectorStoreFactory.createVectorStore(label, index, embeddingModel);
    }


    public List<Document> searchDocuments(VectorStore vectorStore, String queryText, double threshold,int topK) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(queryText)
                .similarityThreshold(threshold)
                .topK(topK)
                .build());
    }

    public String docResult2Prompt(String heading,List<Document> documents){
        StringBuilder sb = new StringBuilder();
        sb.append(heading).append("\n");
        for (Document doc : documents) {
            sb.append("内容片段：").append(doc.getText()).append("\n");
            sb.append("来源：").append(doc.getMetadata().getOrDefault("source", "未知")).append("\n");
            sb.append("-----\n");
        }
        return sb.toString();
    }

    public String graphResult2Prompt(String heading,List<Document> documents){
        StringBuilder sb = new StringBuilder();
        sb.append(heading).append("\n");
        Set<String> sectionIds = new LinkedHashSet<>();
        if(null==documents||documents.isEmpty()){
            sb.append("无相关知识。\n");
            return sb.toString();
        }
        for (Document d : documents) {
            Object sid = d.getMetadata().get("sectionId");
            if (sid instanceof String s && !s.isEmpty()) sectionIds.add(s);
        }
        List<Map<String, Object>> sectionPaths = customerGraphService.getSectionPaths(new ArrayList<>(sectionIds));
        Map<String, String> sectionIdToPath = new HashMap<>();
        for (Map<String, Object> sp : sectionPaths) {
            String sid = Objects.toString(sp.get("sectionId"), "");
            String manual = Objects.toString(sp.get("manualFilePath"), "");
            @SuppressWarnings("unchecked")
            List<String> titles = (List<String>) sp.getOrDefault("sectionPath", Collections.emptyList());
            String pathStr = (manual == null ? "" : manual) + (titles.isEmpty() ? "" : (" > " + String.join(" > ", titles)));
            sectionIdToPath.put(sid, pathStr);
        }

        for (Document d : documents) {
            String text = d.getText();
            Object imgsObj = d.getMetadata().get("imageUrls");
            @SuppressWarnings("unchecked")
            List<String> imgs = (imgsObj instanceof List) ? (List<String>) imgsObj : Collections.emptyList();
            String sid = Objects.toString(d.getMetadata().get("sectionId"), "");
            String pathStr = sectionIdToPath.getOrDefault(sid, "");
            sb.append("###位置：").append(pathStr).append("\n");
            sb.append("###片段：\n").append(text).append("\n");
            if (!imgs.isEmpty()) {
                sb.append("###图片：\n");
                int k = 1;
                for (String url : imgs) {
                    sb.append("- 步骤").append(k).append("图片：<url>").append(url).append("</url>").append("\n");
                    k++;
                }
            }
        }
        return sb.toString();
    }




    public List<Document> importFaqFromFile(String filePath, String tag) {
        var content=ExcelParser.importFile(filePath);
        var sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        var date = sdf.format(new Date());
        List<Document>documents=new ArrayList<>(content.size());
        content.forEach(row-> {
            String question = row[0];
            String answer = row[1];
            String text = "问：" + question + "\n答：" + answer;
            Map<String, Object> md = new HashMap<>();
            md.put("type", "faq");
            md.put("date", date);
            var doc = new Document(text, md);
            documents.add(doc);
        });
        return documents;
    }
}
