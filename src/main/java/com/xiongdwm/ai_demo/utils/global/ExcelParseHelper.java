package com.xiongdwm.ai_demo.utils.global;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Excel 文件解析工具，将 Excel 内容转换为结构化的 Document 列表，
 * 每个 Document 包含若干行数据，格式为 "列名: 值 | 列名: 值"，
 * 便于 LLM 理解表格结构并作为上下文使用。
 */
@Component
public class ExcelParseHelper {

    /** 聊天上下文场景，每个 Document 包含的默认行数 */
    public static final int CHAT_ROWS_PER_CHUNK = 30;
    /** 向量入库场景，每个 Document 包含的默认行数 */
    public static final int EMBEDDING_ROWS_PER_CHUNK = 8;

    /**
     * 解析 Excel 文件，使用默认分块大小（30行，适用于聊天场景）。
     *
     * @param filePath Excel 文件路径（支持 .xlsx 和 .xls）
     * @return 结构化的 Document 列表
     */
    public List<Document> parseExcel(String filePath) throws Exception {
        return parseExcel(filePath, CHAT_ROWS_PER_CHUNK);
    }

    /**
     * 解析 Excel 文件，返回 Document 列表。
     *
     * @param filePath     Excel 文件路径（支持 .xlsx 和 .xls）
     * @param rowsPerChunk 每个 Document 包含的最大行数
     * @return 结构化的 Document 列表
     */
    public List<Document> parseExcel(String filePath, int rowsPerChunk) throws Exception {
        List<Document> documents = new ArrayList<>();

        try (InputStream is = new FileInputStream(filePath);
             Workbook workbook = createWorkbook(is, filePath)) {

            DataFormatter formatter = new DataFormatter();

            for (int sheetIdx = 0; sheetIdx < workbook.getNumberOfSheets(); sheetIdx++) {
                Sheet sheet = workbook.getSheetAt(sheetIdx);
                String sheetName = sheet.getSheetName();

                if (sheet.getPhysicalNumberOfRows() == 0) continue;

                // 读取表头（第一行）
                Row headerRow = sheet.getRow(sheet.getFirstRowNum());
                if (headerRow == null) continue;

                List<String> headers = new ArrayList<>();
                int lastCol = headerRow.getLastCellNum();
                for (int col = 0; col < lastCol; col++) {
                    Cell cell = headerRow.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String value = formatter.formatCellValue(cell).trim();
                    headers.add(value.isEmpty() ? ("列" + (col + 1)) : value);
                }

                // 读取数据行，按 rowsPerChunk 分组
                List<String> rowTexts = new ArrayList<>();
                int dataRowStart = sheet.getFirstRowNum() + 1;
                int dataRowEnd = sheet.getLastRowNum();

                for (int rowIdx = dataRowStart; rowIdx <= dataRowEnd; rowIdx++) {
                    Row row = sheet.getRow(rowIdx);
                    if (row == null) continue;

                    // 跳过全空行
                    boolean allBlank = true;
                    StringBuilder rowBuilder = new StringBuilder();
                    for (int col = 0; col < lastCol; col++) {
                        Cell cell = row.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        String value = formatter.formatCellValue(cell).trim();
                        if (!value.isEmpty()) allBlank = false;

                        if (col > 0) rowBuilder.append(" | ");
                        rowBuilder.append(headers.get(Math.min(col, headers.size() - 1)))
                                  .append(": ")
                                  .append(value);
                    }
                    if (allBlank) continue;
                    rowTexts.add(rowBuilder.toString());

                    // 达到分组上限，生成一个 Document
                    if (rowTexts.size() >= rowsPerChunk) {
                        documents.add(buildDocument(sheetName, headers, rowTexts, sheetIdx, documents.size()));
                        rowTexts = new ArrayList<>();
                    }
                }

                // 处理剩余行
                if (!rowTexts.isEmpty()) {
                    documents.add(buildDocument(sheetName, headers, rowTexts, sheetIdx, documents.size()));
                }
            }
        }

        return documents;
    }

    /**
     * 判断文件名是否为 Excel 格式
     */
    public static boolean isExcelFile(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        return lower.endsWith(".xlsx") || lower.endsWith(".xls");
    }

    private Document buildDocument(String sheetName, List<String> headers,
                                   List<String> rowTexts, int sheetIndex, int chunkIndex) {
        StringBuilder text = new StringBuilder();
        text.append("[Sheet: ").append(sheetName).append("]\n");
        text.append("[表头: ").append(String.join(" | ", headers)).append("]\n");
        for (String row : rowTexts) {
            text.append(row).append("\n");
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("sheetName", sheetName);
        metadata.put("sheetIndex", sheetIndex);
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("rowCount", rowTexts.size());
        metadata.put("source", "excel");

        return new Document(text.toString(), metadata);
    }

    private Workbook createWorkbook(InputStream is, String filePath) throws Exception {
        if (filePath.toLowerCase().endsWith(".xlsx")) {
            return new XSSFWorkbook(is);
        } else if (filePath.toLowerCase().endsWith(".xls")) {
            return new HSSFWorkbook(is);
        }
        throw new IllegalArgumentException("不支持的文件格式: " + filePath);
    }
}
