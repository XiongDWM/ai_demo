package com.xiongdwm.ai_demo.utils.global;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

public class ExcelParser {
    public Map<String, String> parseExcelFile(String filePath) {
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
                        .limit(20)
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

    private String determineCellType(Cell cell) {
        if (cell == null) {
            return "null";
        }
        switch (cell.getCellType()) {
            case STRING:
                return "varchar";
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return "datetime";
                } else {
                    return "double"; 
                }
            case BOOLEAN:
                return "tinyint"; 
            case FORMULA:
                return "formula";
            default:
                return "varchar"; 
        }
    }

    public static String inferColumnType(List<String> columnValues) {
        boolean isInt = true, isDouble = true, isDate = true, isBigInt = true;
        SimpleDateFormat[] dateFormats = {
                new SimpleDateFormat("yyyy-MM-dd"),
                new SimpleDateFormat("yyyy/MM/dd"),
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss")
        };

        for (String value : columnValues) {
            if (value == null || value.trim().isEmpty())
                continue;
            // 整数
            try {
                Integer.parseInt(value);
            } catch (Exception e) {
                isInt = false;
            }
            try {
                Long.parseLong(value);
            } catch (Exception e) {
                isBigInt = false;
            }
            // 小数
            try {
                Double.parseDouble(value);
            } catch (Exception e) {
                isDouble = false;
            }
            // 日期
            boolean matched = false;
            for (SimpleDateFormat fmt : dateFormats) {
                try {
                    fmt.setLenient(false);
                    fmt.parse(value);
                    matched = true;
                    break;
                } catch (Exception ignore) {
                }
            }
            if (!matched)
                isDate = false;
        }
        if (isInt)
            return "INT";
        if (isBigInt)
            return "BIGINT";
        if (isDouble)
            return "DOUBLE";
        if (isDate)
            return "DATETIME";
        return "VARCHAR(255)";
    }

    public static void main(String[] args) {
        ExcelParser parser = new ExcelParser();
        Map<String, String> result = parser.parseExcelFile("/Users/xiong/Files/ss/系统输出报表/铁塔.xlsx");
        System.out.println(result);
        var i=Math.max(1,7);
        System.out.println(i);
    }

}
