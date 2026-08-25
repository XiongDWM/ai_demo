package com.xiongdwm.ai_demo.utils.global;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.StreamSupport;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Excel 动态建表 + 数据导入工具。
 * 解析 Excel → 推断列类型 → 执行 CREATE TABLE → 批量 INSERT → 生成表结构描述。
 */
@Component
public class ExcelParser {

    /**
     * 解析结果：包含表名、DDL、INSERT 语句列表、表结构描述文本
     */
    public record ParseResult(
            String tableName,
            String tableNameCN,
            String ddl,
            List<String> insertSqls,
            String dbDescription,
            Map<String, String> columnMap
    ) {}

    public static List<String[]> importFile(String filePath){
        List<String[]> columnValues=new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                var rowValues=new String[row.getLastCellNum()];
                for (Cell cell : row) {
                    var value = cell.toString();
                    rowValues[cell.getColumnIndex()]=value;
                }
                columnValues.addLast(rowValues);
            }
            return columnValues;
        } catch (IOException e) {
            throw new RuntimeException("文件不存在或无法读取: " + filePath, e);
        } catch (Exception e) {
            throw new RuntimeException("Excel文件格式错误: " + e.getMessage(), e);
        }
    }


    public Map<String, String> parseExcelFileOld(String filePath) {
        String tableNameCN=filePath.substring(filePath.lastIndexOf("/")+1,filePath.lastIndexOf("."));
        try (FileInputStream fis = new FileInputStream(filePath);
                Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel文件表头为空");
            }
            Map<String, String> columnMap = new HashMap<>();
            for (Cell cell : headerRow) {
                int colIndex = cell.getColumnIndex();
                String colLetter = CellReference.convertNumToColString(colIndex);
                String colName = cell.getStringCellValue();
                columnMap.put(colLetter, colName);
            }
            // 每一列取20行数据传入inferColumnType方法，判断数据类型
            String tableName = "temp_table_" + System.currentTimeMillis();
            StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (\n");
            ddl.append("  `id` INT AUTO_INCREMENT PRIMARY KEY,\n");
            StringBuilder dbDescription = new StringBuilder();
            dbDescription.append("###").append(tableName).append(" ");
            dbDescription.append(tableNameCN).append("\n");
            for (String colLetter : columnMap.keySet()) {
                int colIndex = CellReference.convertColStringToIndex(colLetter);
                List<String> columnValues = StreamSupport.stream(sheet.spliterator(), true)
                        .skip(1)
//                        .limit(20)
                        .map(row -> {
                            Cell cell = row.getCell(colIndex);
                            if (cell != null) {
                                return cell.toString();
                            }
                            return null;
                        })
                        .toList();
                String inferredType = inferColumnType(columnValues);
                ddl.append("  `").append(colLetter).append("` ");
                ddl.append(inferredType).append(" COMMENT '").append(columnMap.get(colLetter)).append("',\n");
                columnMap.put(colLetter, inferredType + "," + columnMap.get(colLetter));
                dbDescription.append(" - ").append(colLetter).append(": ")
                        .append(columnMap.get(colLetter)).append("\n");
            }
            if (ddl.lastIndexOf(",") == ddl.length() - 2) {
                ddl.deleteCharAt(ddl.length() - 2);
            }
            ddl.append(");\n");
            // System.out.println(ddl);

            System.out.println(dbDescription);
            return columnMap;
        } catch (IOException e) {
            throw new RuntimeException("文件不存在或无法读取: " + filePath, e);
        } catch (Exception e) {
            throw new RuntimeException("Excel文件格式错误: " + e.getMessage(), e);
        }
    }

    /**
     * 完整解析 Excel 文件，返回 DDL + INSERT SQL + 描述文本。
     */
    public ParseResult parseExcelFull(String filePath) {
        String tableNameCN = filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf("."));
        // Windows 路径兼容
        if (tableNameCN.contains("\\")) {
            tableNameCN = tableNameCN.substring(tableNameCN.lastIndexOf("\\") + 1);
        }

        try (InputStream is = new FileInputStream(filePath);
             Workbook workbook = createWorkbook(is, filePath)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("Excel文件表头为空");
            }

            DataFormatter formatter = new DataFormatter();

            // 1. 读取表头
            Map<String, String> columnMap = new LinkedHashMap<>();
            List<String> colLetters = new ArrayList<>();
            for (Cell cell : headerRow) {
                int colIndex = cell.getColumnIndex();
                String colLetter = CellReference.convertNumToColString(colIndex);
                String colName = formatter.formatCellValue(cell).trim();
                if (colName.isEmpty()) colName = "col_" + colLetter;
                columnMap.put(colLetter, colName);
                colLetters.add(colLetter);
            }

            // 2. 推断列类型
            Map<String, String> colTypes = new LinkedHashMap<>();
            for (String colLetter : colLetters) {
                int colIndex = CellReference.convertColStringToIndex(colLetter);
                List<String> columnValues = StreamSupport.stream(sheet.spliterator(), false)
                        .skip(1)
                        .map(row -> {
                            Cell cell = row.getCell(colIndex);
                            return cell != null ? formatter.formatCellValue(cell).trim() : null;
                        })
                        .toList();
                colTypes.put(colLetter, inferColumnType(columnValues));
            }

            // 3. 生成表名和 DDL
            String tableName = "eval_" + System.currentTimeMillis();
            StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (\n");
            ddl.append("  `id` INT AUTO_INCREMENT PRIMARY KEY,\n");
            for (String col : colLetters) {
                ddl.append("  `").append(col).append("` ");
                ddl.append(colTypes.get(col));
                ddl.append(" COMMENT '").append(columnMap.get(col)).append("',\n");
            }
            // 去掉最后一个逗号
            if (ddl.lastIndexOf(",") == ddl.length() - 2) {
                ddl.deleteCharAt(ddl.length() - 2);
            }
            ddl.append(");\n");

            // 4. 生成 INSERT 语句
            List<String> insertSqls = new ArrayList<>();
            int batchSize = 200;
            List<String> valueBatch = new ArrayList<>();

            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                boolean allBlank = true;
                StringBuilder values = new StringBuilder("(");
                for (int i = 0; i < colLetters.size(); i++) {
                    String col = colLetters.get(i);
                    int colIndex = CellReference.convertColStringToIndex(col);
                    Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    String value = formatter.formatCellValue(cell).trim();
                    if (!value.isEmpty()) allBlank = false;

                    if (i > 0) values.append(", ");
                    String type = colTypes.get(col);
                    if (value.isEmpty()) {
                        values.append("NULL");
                    } else if ("INT".equals(type) || "BIGINT".equals(type) || "DOUBLE".equals(type)) {
                        values.append(value);
                    } else {
                        values.append("'").append(value.replace("'", "''")).append("'");
                    }
                }
                values.append(")");
                if (allBlank) continue;
                valueBatch.add(values.toString());

                if (valueBatch.size() >= batchSize) {
                    insertSqls.add(buildInsertSql(tableName, colLetters, valueBatch));
                    valueBatch = new ArrayList<>();
                }
            }
            if (!valueBatch.isEmpty()) {
                insertSqls.add(buildInsertSql(tableName, colLetters, valueBatch));
            }

            // 5. 生成结构描述文本（用于向量化）
            StringBuilder desc = new StringBuilder();
            desc.append("### 表名: ").append(tableName).append(" (").append(tableNameCN).append(")\n");
            for (String col : colLetters) {
                desc.append(" - 列 `").append(col).append("`: ")
                        .append(columnMap.get(col))
                        .append(" (类型: ").append(colTypes.get(col)).append(")\n");
            }

            // 更新 columnMap 为 "类型,列名" 格式供兼容
            for (String col : colLetters) {
                columnMap.put(col, colTypes.get(col) + "," + columnMap.get(col));
            }

            return new ParseResult(tableName, tableNameCN, ddl.toString(), insertSqls, desc.toString(), columnMap);

        } catch (IOException e) {
            throw new RuntimeException("文件不存在或无法读取: " + filePath, e);
        } catch (Exception e) {
            throw new RuntimeException("Excel文件格式错误: " + e.getMessage(), e);
        }
    }

    private Workbook createWorkbook(InputStream is, String filePath) throws Exception {
        if (filePath.toLowerCase().endsWith(".xlsx")) {
            return new XSSFWorkbook(is);
        } else if (filePath.toLowerCase().endsWith(".xls")) {
            return new HSSFWorkbook(is);
        }
        throw new IllegalArgumentException("不支持的文件格式: " + filePath);
    }

    private String buildInsertSql(String tableName, List<String> colLetters, List<String> valueBatch) {
        StringBuilder sql = new StringBuilder("INSERT INTO `").append(tableName).append("` (");
        for (int i = 0; i < colLetters.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("`").append(colLetters.get(i)).append("`");
        }
        sql.append(") VALUES\n");
        for (int i = 0; i < valueBatch.size(); i++) {
            if (i > 0) sql.append(",\n");
            sql.append(valueBatch.get(i));
        }
        sql.append(";");
        return sql.toString();
    }

    /**
     * 兼容老接口
     */
    public Map<String, String> parseExcelFile(String filePath) {
        return parseExcelFull(filePath).columnMap();
    }

    public static String inferColumnType(List<String> columnValues) {
        boolean isInt = true, isDouble = true, isDate = true, isBigInt = true;
        boolean hasNonEmpty = false;
        SimpleDateFormat[] dateFormats = {
                new SimpleDateFormat("yyyy-MM-dd"),
                new SimpleDateFormat("yyyy/MM/dd"),
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        };

        for (String value : columnValues) {
            if (value == null || value.trim().isEmpty())
                continue;
            hasNonEmpty = true;
            try { Integer.parseInt(value); } catch (Exception e) { isInt = false; }
            try { Long.parseLong(value); } catch (Exception e) { isBigInt = false; }
            try { Double.parseDouble(value); } catch (Exception e) { isDouble = false; }
            boolean matched = false;
            for (SimpleDateFormat fmt : dateFormats) {
                try { fmt.setLenient(false); fmt.parse(value); matched = true; break; } catch (Exception ignore) {}
            }
            if (!matched) isDate = false;
        }
        if (!hasNonEmpty) return "VARCHAR(255)";
        if (isInt) return "INT";
        if (isBigInt) return "BIGINT";
        if (isDouble) return "DOUBLE";
        if (isDate) return "DATETIME";
        return "VARCHAR(255)";
    }
}
