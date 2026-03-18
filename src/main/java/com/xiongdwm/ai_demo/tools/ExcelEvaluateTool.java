package com.xiongdwm.ai_demo.tools;

import java.util.Collections;
import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.xiongdwm.ai_demo.ingest.ExcelEvaluateService;

/**
 * Excel 评估 Agent 的工具集。
 * 供 Spring AI Agent 自动调用，实现：
 *   1. 检索表结构描述（含跨表关系）
 *   2. 执行 SQL 查询
 *   3. 导出查询结果为 Excel
 */
@Component
public class ExcelEvaluateTool {

    @Autowired
    private ExcelEvaluateService evaluateService;

    @Value("${file.upload.path}")
    private String uploadPath;

    /**
     * snapshotId 需要在 Agent 调用前通过 ThreadLocal 或请求上下文注入。
     * 这里用 ThreadLocal 来传递当前评估的 snapshotId。
     */
    private static final ThreadLocal<Long> CURRENT_SNAPSHOT_ID = new ThreadLocal<>();

    public static void setCurrentSnapshotId(Long snapshotId) {
        CURRENT_SNAPSHOT_ID.set(snapshotId);
    }

    public static void clearCurrentSnapshotId() {
        CURRENT_SNAPSHOT_ID.remove();
    }

    @Tool(name = "excelDbDescriptionRetrieve",
          description = "根据用户问题检索已上传 Excel 对应的数据库表结构描述和跨表关联关系，返回相关的表名、字段名、字段类型和关联信息，用于生成 SQL")
    public String excelDbDescriptionRetrieve(
            @ToolParam(description = "用户输入的问题") String message) {
        Long snapshotId = CURRENT_SNAPSHOT_ID.get();
        if (snapshotId == null) return "错误：未指定评估快照ID";

        List<Document> docs = evaluateService.searchDescriptions(snapshotId, message);
        if (docs == null || docs.isEmpty()) return "未找到相关表结构描述";

        StringBuilder sb = new StringBuilder();
        for (Document doc : docs) {
            sb.append(doc.getText()).append("\n");
        }
        System.out.println(sb.toString());
        return sb.toString();
    }

    @Tool(name = "excelSqlExecute",
          description = "执行 SQL 查询语句并返回查询结果。SQL 必须是 SELECT 查询，不允许执行 INSERT/UPDATE/DELETE/DROP 等修改操作")
    public String excelSqlExecute(
            @ToolParam(description = "要执行的 SQL SELECT 查询语句") String sql) {
        // 安全检查：只允许 SELECT
        System.out.println("执行 sql=" + sql);
        String trimmed = sql.trim().toUpperCase();
        if (!trimmed.startsWith("SELECT")) {
            return "安全限制：只允许执行 SELECT 查询";
        }

        Long snapshotId = CURRENT_SNAPSHOT_ID.get();
        try {
            String result = evaluateService.executeSql(sql);
            System.out.println("sql=" + sql + ", result=" + result.replace("\n", ","));
            // 记录最近的 SQL
            if (snapshotId != null) {
                evaluateService.updateLastSql(snapshotId, null, sql);
            }
            if (result.isEmpty()) return "查询结果为空";
            return result;
        } catch (Exception e) {
            return "SQL 执行失败: " + e.getMessage();
        }
    }

    @Tool(name = "exportResultToExcel",
          description = "将上一次的 SQL 查询结果导出为 Excel 文件，返回导出文件的路径。仅在用户明确要求导出时调用")
    public String exportResultToExcel(
            @ToolParam(description = "要导出结果的 SQL 查询语句") String sql) {
        String trimmed = sql.trim().toUpperCase();
        if (!trimmed.startsWith("SELECT")) {
            return "安全限制：只允许导出 SELECT 查询结果";
        }
        try {
            String exportDir = uploadPath + "/export";
            String filePath = evaluateService.exportToExcel(sql, exportDir);
            return "已成功导出 Excel 文件: " + filePath;
        } catch (Exception e) {
            return "导出失败: " + e.getMessage();
        }
    }
}
