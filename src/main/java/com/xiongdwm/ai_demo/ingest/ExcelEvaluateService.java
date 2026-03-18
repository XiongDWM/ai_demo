package com.xiongdwm.ai_demo.ingest;

import java.util.*;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.xiongdwm.ai_demo.utils.config.Neo4jVectorStoreFactory;
import com.xiongdwm.ai_demo.utils.global.ExcelParser;
import com.xiongdwm.ai_demo.utils.global.ExcelParser.ParseResult;
import com.xiongdwm.ai_demo.webapp.entities.EvaluateExcelLog;
import com.xiongdwm.ai_demo.webapp.entities.EvaluateSnapshot;
import com.xiongdwm.ai_demo.webapp.repository.EvaluateExcelLogRepository;
import com.xiongdwm.ai_demo.webapp.repository.EvaluateSnapshotRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Excel 评估服务：
 * 1. 上传 Excel → 动态建表 + 插入数据
 * 2. 注册表结构描述 + 跨表关系到独立向量库
 * 3. 查询/导出支持
 */
@Service
public class ExcelEvaluateService {

    @Autowired
    private ExcelParser excelParser;
    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private Neo4jVectorStoreFactory vectorStoreFactory;
    @Autowired
    private EvaluateSnapshotRepository snapshotRepository;
    @Autowired
    private EvaluateExcelLogRepository excelLogRepository;
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 创建一次评估快照（不含 Excel，先创建拿到 ID 用于生成 vectorTag）
     */
    @Transactional
    public EvaluateSnapshot createSnapshot(String tableRelation) {
        EvaluateSnapshot snapshot = new EvaluateSnapshot();
        snapshot.setTableRelation(tableRelation);
        snapshot.setCreateDate(new Date());
        snapshot = snapshotRepository.save(snapshot);
        snapshot.generateVectorTag();
        snapshot = snapshotRepository.save(snapshot);
        return snapshot;
    }

    /**
     * 单独上传一个 Excel 文件：解析 → 动态建表 → 插入数据 → 保存日志（尚未关联 Snapshot）。
     * 返回保存后的 EvaluateExcelLog（含 id）。
     */
    @Transactional
    public EvaluateExcelLog uploadExcel(String filePath, String fileName) {
        ParseResult result = excelParser.parseExcelFull(filePath);

        // 1. 执行 DDL 建表
        entityManager.createNativeQuery(result.ddl()).executeUpdate();
        System.out.println("已创建表: " + result.tableName());

        // 2. 批量 INSERT 数据
        for (String insertSql : result.insertSqls()) {
            entityManager.createNativeQuery(insertSql).executeUpdate();
        }
        System.out.println("已插入数据到: " + result.tableName() + ", 共 " + result.insertSqls().size() + " 批");

        // 3. 记录 ExcelLog（snapshotId 暂不关联）
        EvaluateExcelLog log = new EvaluateExcelLog();
        log.setFileName(fileName);
        log.setFilePath(filePath);
        log.setTableName(result.tableName());
        log.setDbDescription(result.dbDescription());
        log.setUploadDate(new Date());
        return excelLogRepository.save(log);
    }

    /**
     * 创建快照并关联已上传的 ExcelLog：
     * 1. 创建 Snapshot
     * 2. 将 excelLogIds 对应的日志关联到该 Snapshot
     * 3. 注册所有表描述 + 跨表关系到专属向量库
     */
    @Transactional
    public EvaluateSnapshot setupSnapshot(String tableRelation, List<Long> excelLogIds) {
        // 1. 创建快照
        EvaluateSnapshot snapshot = createSnapshot(tableRelation);

        // 2. 关联 ExcelLog
        List<EvaluateExcelLog> logs = excelLogRepository.findAllById(excelLogIds);
        for (EvaluateExcelLog log : logs) {
            log.setSnapshotId(snapshot.getId());
        }
        excelLogRepository.saveAll(logs);

        // 3. 注册描述到向量库
        registerDescriptions(snapshot);

        return snapshot;
    }

    /**
     * 查询所有未关联 Snapshot 的 ExcelLog（snapshotId 为 null），即待选列表
     */
    public List<EvaluateExcelLog> listUnboundExcelLogs() {
        return excelLogRepository.findBySnapshotIdIsNull();
    }

    /**
     * 将所有表描述 + 跨表关系写入该 Snapshot 专属的向量库。
     */
    public void registerDescriptions(EvaluateSnapshot snapshot) {
        String tag = snapshot.getVectorTag();
        vectorStoreFactory.createVectorIndex(tag, tag, 768, "embedding", "COSINE");
        VectorStore vectorStore = vectorStoreFactory.createVectorStore(tag, tag, embeddingModel);

        List<Document> docs = new ArrayList<>();

        // 每张表的描述
        List<EvaluateExcelLog> logs = excelLogRepository.findBySnapshotId(snapshot.getId());
        for (EvaluateExcelLog log : logs) {
            if (log.getDbDescription() != null && !log.getDbDescription().isEmpty()) {
                docs.add(new Document(log.getDbDescription(),
                        Map.of("source", "table_desc", "tableName", log.getTableName())));
            }
        }

        // 跨表关系描述
        if (snapshot.getTableRelation() != null && !snapshot.getTableRelation().isEmpty()) {
            // 构建包含实际表名映射的关系描述
            StringBuilder relationDesc = new StringBuilder();
            relationDesc.append("### 跨表关联关系\n");
            relationDesc.append(snapshot.getTableRelation()).append("\n");
            relationDesc.append("### 表名映射\n");
            for (EvaluateExcelLog log : logs) {
                relationDesc.append(" - ").append(log.getFileName())
                        .append(" → 数据库表名: `").append(log.getTableName()).append("`\n");
            }
            docs.add(new Document(relationDesc.toString(),
                    Map.of("source", "table_relation")));
        }

        if (!docs.isEmpty()) {
            vectorStore.add(docs);
            System.out.println("已注册 " + docs.size() + " 条描述到向量库 " + tag);
        }
    }

    /**
     * 根据 snapshotId 检索表结构描述（向量搜索）
     */
    public List<Document> searchDescriptions(Long snapshotId, String query) {
        EvaluateSnapshot snapshot = snapshotRepository.findById(snapshotId).orElse(null);
        if (snapshot == null || snapshot.getVectorTag() == null) return Collections.emptyList();

        String tag = snapshot.getVectorTag();
        VectorStore vectorStore = vectorStoreFactory.createVectorStore(tag, tag, embeddingModel);
        var documents=vectorStore.similaritySearch(
                org.springframework.ai.vectorstore.SearchRequest.builder()
                        .query(query)
                        .topK(20)
                        .similarityThreshold(0.5)
                        .build());
        documents.forEach(doc -> System.out.println("检索到相关描述: " + doc.getText()));
        return documents;
    }

    /**
     * 执行 SQL 查询，返回结果文本
     */
    public String executeSql(String sql) {
        var query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> resultList = query.getResultList();
        StringBuilder sb = new StringBuilder();
        for (Object result : resultList) {
            if (result instanceof Object[] row) {
                sb.append(java.util.Arrays.toString(row)).append("\n");
            } else {
                sb.append(result.toString()).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 执行 SQL 查询，返回结构化结果（用于导出 Excel）
     */
    public List<List<String>> executeSqlAsRows(String sql) {
        var query = entityManager.createNativeQuery(sql);
        @SuppressWarnings("unchecked")
        List<Object[]> resultList = query.getResultList();
        List<List<String>> rows = new ArrayList<>();
        for (Object result : resultList) {
            List<String> row = new ArrayList<>();
            if (result instanceof Object[] arr) {
                for (Object val : arr) {
                    row.add(val == null ? "" : val.toString());
                }
            } else {
                row.add(result == null ? "" : result.toString());
            }
            rows.add(row);
        }
        return rows;
    }

    /**
     * 导出查询结果为 Excel 文件，返回文件路径
     */
    public String exportToExcel(String sql, String exportDir) throws Exception {
        List<List<String>> rows = executeSqlAsRows(sql);
        if (rows.isEmpty()) throw new RuntimeException("查询结果为空，无法导出");

        java.nio.file.Files.createDirectories(java.nio.file.Path.of(exportDir));
        String fileName = "export_" + System.currentTimeMillis() + ".xlsx";
        String filePath = exportDir + "/" + fileName;

        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             var fos = new java.io.FileOutputStream(filePath)) {
            var sheet = workbook.createSheet("查询结果");
            for (int i = 0; i < rows.size(); i++) {
                var row = sheet.createRow(i);
                List<String> cells = rows.get(i);
                for (int j = 0; j < cells.size(); j++) {
                    row.createCell(j).setCellValue(cells.get(j));
                }
            }
            workbook.write(fos);
        }
        System.out.println("已导出 Excel: " + filePath);
        return filePath;
    }

    /**
     * 更新 snapshot 的最近 SQL
     */
    @Transactional
    public void updateLastSql(Long snapshotId, String question, String sql) {
        snapshotRepository.findById(snapshotId).ifPresent(s -> {
            s.setLastQuestion(question);
            s.setLastSql(sql);
            snapshotRepository.save(s);
        });
    }

    /**
     * 清理 snapshot 关联的临时表
     */
    @Transactional
    public void cleanupSnapshot(Long snapshotId) {
        List<EvaluateExcelLog> logs = excelLogRepository.findBySnapshotId(snapshotId);
        for (EvaluateExcelLog log : logs) {
            if (log.getTableName() != null) {
                try {
                    entityManager.createNativeQuery("DROP TABLE IF EXISTS `" + log.getTableName() + "`").executeUpdate();
                    System.out.println("已删除临时表: " + log.getTableName());
                } catch (Exception e) {
                    System.err.println("删除临时表失败: " + log.getTableName() + " - " + e.getMessage());
                }
            }
        }
    }

    public EvaluateSnapshot getSnapshot(Long snapshotId) {
        return snapshotRepository.findById(snapshotId).orElse(null);
    }

    /**
     * 查询所有评估快照（不分页）
     */
    public List<EvaluateSnapshot> listSnapshots() {
        return snapshotRepository.findAll();
    }

    /**
     * 查询指定 snapshot 下的所有 Excel 上传记录（不分页）
     */
    public List<EvaluateExcelLog> listExcelLogs(Long snapshotId) {
        return excelLogRepository.findBySnapshotId(snapshotId);
    }
}
