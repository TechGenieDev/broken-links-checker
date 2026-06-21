package com.qa.linkchecker.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the list of pages to scan from the input Excel file.
 * Expects a header row with a column whose name matches input.column.name (e.g. "URL").
 */
public class ExcelReader {

    public static List<String> readPageList(String filePath, String sheetName, String columnName) {
        List<String> pages = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = (sheetName == null || sheetName.isBlank())
                    ? workbook.getSheetAt(0)
                    : workbook.getSheet(sheetName);

            if (sheet == null) {
                throw new RuntimeException("Sheet '" + sheetName + "' not found in " + filePath);
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new RuntimeException("Header row is missing in " + filePath);
            }

            int columnIndex = -1;
            for (Cell cell : headerRow) {
                if (cell.getCellType() == CellType.STRING
                        && cell.getStringCellValue().trim().equalsIgnoreCase(columnName)) {
                    columnIndex = cell.getColumnIndex();
                    break;
                }
            }
            if (columnIndex == -1) {
                throw new RuntimeException(
                        "Column '" + columnName + "' not found in the header row of " + filePath);
            }

            DataFormatter formatter = new DataFormatter();
            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    continue;
                }
                Cell cell = row.getCell(columnIndex);
                if (cell == null) {
                    continue;
                }

                String value = formatter.formatCellValue(cell).trim();
                if (value.isEmpty()) {
                    continue;
                }

                if (!value.startsWith("http://") && !value.startsWith("https://")) {
                    System.out.println("Skipping invalid URL (row " + (rowIdx + 1) + "): " + value);
                    continue;
                }
                pages.add(value);
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read input Excel file: " + filePath + " -> " + e.getMessage(), e);
        }

        return pages;
    }
}
