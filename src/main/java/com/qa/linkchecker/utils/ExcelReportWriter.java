package com.qa.linkchecker.utils;

import com.qa.linkchecker.model.LinkResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates the final Excel report:
 *  - "Summary" sheet with totals
 *  - "Link Details" sheet with every link checked, color-coded by status
 */
public class ExcelReportWriter {

    private static final String[] HEADERS = {
            "Source Page", "Link Text", "Link URL", "Status Code", "Status", "Response Time (ms)", "Remarks"
    };

    public static void writeReport(String filePath, List<LinkResult> results) {
        try (Workbook workbook = new XSSFWorkbook()) {

            writeSummarySheet(workbook, results);
            writeDetailSheet(workbook, results);

            File outFile = new File(filePath);
            File parent = outFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                workbook.write(fos);
            }

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to write report Excel file: " + filePath + " -> " + e.getMessage(), e);
        }
    }

    private static void writeDetailSheet(Workbook workbook, List<LinkResult> results) {
        Sheet sheet = workbook.createSheet("Link Details");

        CellStyle headerStyle = headerStyle(workbook);
        CellStyle okStyle = coloredStyle(workbook, IndexedColors.LIGHT_GREEN);
        CellStyle brokenStyle = coloredStyle(workbook, IndexedColors.ROSE);
        CellStyle redirectStyle = coloredStyle(workbook, IndexedColors.LIGHT_YELLOW);
        CellStyle skippedStyle = coloredStyle(workbook, IndexedColors.GREY_25_PERCENT);
        CellStyle errorStyle = coloredStyle(workbook, IndexedColors.ORANGE);

        Row header = sheet.createRow(0);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (LinkResult r : results) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(r.getSourcePage());
            row.createCell(1).setCellValue(r.getLinkText());
            row.createCell(2).setCellValue(r.getLinkUrl());
            row.createCell(3).setCellValue(r.getStatusCode());
            row.createCell(4).setCellValue(r.getStatus());
            row.createCell(5).setCellValue(r.getResponseTimeMs());
            row.createCell(6).setCellValue(r.getRemarks() == null ? "" : r.getRemarks());

            CellStyle rowStyle;
            switch (r.getStatus()) {
                case "OK":
                    rowStyle = okStyle;
                    break;
                case "BROKEN":
                    rowStyle = brokenStyle;
                    break;
                case "REDIRECT":
                    rowStyle = redirectStyle;
                    break;
                case "SKIPPED":
                    rowStyle = skippedStyle;
                    break;
                default:
                    rowStyle = errorStyle;
            }
            for (int c = 0; c < HEADERS.length; c++) {
                row.getCell(c).setCellStyle(rowStyle);
            }
        }

        sheet.createFreezePane(0, 1);
        if (rowIdx > 1) {
            sheet.setAutoFilter(new CellRangeAddress(0, rowIdx - 1, 0, HEADERS.length - 1));
        }
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static void writeSummarySheet(Workbook workbook, List<LinkResult> results) {
        Sheet sheet = workbook.createSheet("Summary");
        CellStyle headerStyle = headerStyle(workbook);

        long total = results.size();
        long ok = results.stream().filter(r -> "OK".equals(r.getStatus())).count();
        long broken = results.stream().filter(r -> "BROKEN".equals(r.getStatus())).count();
        long redirect = results.stream().filter(r -> "REDIRECT".equals(r.getStatus())).count();
        long skipped = results.stream().filter(r -> "SKIPPED".equals(r.getStatus())).count();
        long error = results.stream().filter(r -> "ERROR".equals(r.getStatus())).count();

        String[][] summaryRows = {
                {"Report Generated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))},
                {"Total Links Checked", String.valueOf(total)},
                {"OK", String.valueOf(ok)},
                {"Broken", String.valueOf(broken)},
                {"Redirect", String.valueOf(redirect)},
                {"Skipped", String.valueOf(skipped)},
                {"Error / Unreachable", String.valueOf(error)},
        };

        int rowIdx = 0;
        Row title = sheet.createRow(rowIdx++);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("Broken Link Checker - Summary");
        titleCell.setCellStyle(headerStyle);

        rowIdx++;
        for (String[] entry : summaryRows) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(entry[0]);
            row.createCell(1).setCellValue(entry[1]);
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private static CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private static CellStyle coloredStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
