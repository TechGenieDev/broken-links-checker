package com.qa.linkchecker.utils;

import com.qa.linkchecker.model.LinkResult;
import org.apache.poi.ss.usermodel.*;
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
            "Resource Type", "Source Page", "Link/Image Text",
            "URL", "Status Code", "Status", "Response Time (ms)", "Remarks"
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
            row.createCell(0).setCellValue(r.getResourceType());
            row.createCell(1).setCellValue(r.getSourcePage());
            row.createCell(2).setCellValue(r.getLinkText());
            row.createCell(3).setCellValue(r.getLinkUrl());
            row.createCell(4).setCellValue(r.getStatusCode());
            row.createCell(5).setCellValue(r.getStatus());
            row.createCell(6).setCellValue(r.getResponseTimeMs());
            row.createCell(7).setCellValue(r.getRemarks() == null ? "" : r.getRemarks());

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

        // ---------------------------
        // Styles
        // ---------------------------
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        titleStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        titleStyle.setWrapText(true);
        titleFont.setColor(IndexedColors.WHITE.getIndex());
        titleStyle.setBorderTop(BorderStyle.THIN);
        titleStyle.setBorderBottom(BorderStyle.THIN);
        titleStyle.setBorderLeft(BorderStyle.THIN);
        titleStyle.setBorderRight(BorderStyle.THIN);

        CellStyle sectionHeaderStyle = workbook.createCellStyle();
        Font sectionHeaderFont = workbook.createFont();
        sectionHeaderFont.setBold(true);
        sectionHeaderFont.setFontHeightInPoints((short) 12);
        sectionHeaderStyle.setFont(sectionHeaderFont);
        sectionHeaderStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        sectionHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        sectionHeaderStyle.setAlignment(HorizontalAlignment.LEFT);
        sectionHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        sectionHeaderStyle.setBorderTop(BorderStyle.THIN);
        sectionHeaderStyle.setBorderBottom(BorderStyle.THIN);
        sectionHeaderStyle.setBorderLeft(BorderStyle.THIN);
        sectionHeaderStyle.setBorderRight(BorderStyle.THIN);

        CellStyle labelStyle = workbook.createCellStyle();
        Font labelFont = workbook.createFont();
        labelFont.setBold(true);
        labelStyle.setFont(labelFont);
        labelStyle.setBorderTop(BorderStyle.THIN);
        labelStyle.setBorderBottom(BorderStyle.THIN);
        labelStyle.setBorderLeft(BorderStyle.THIN);
        labelStyle.setBorderRight(BorderStyle.THIN);
        labelStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle valueStyle = workbook.createCellStyle();
        valueStyle.setBorderTop(BorderStyle.THIN);
        valueStyle.setBorderBottom(BorderStyle.THIN);
        valueStyle.setBorderLeft(BorderStyle.THIN);
        valueStyle.setBorderRight(BorderStyle.THIN);
        valueStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle percentStyle = workbook.createCellStyle();
        percentStyle.cloneStyleFrom(valueStyle);
        DataFormat dataFormat = workbook.createDataFormat();
        percentStyle.setDataFormat(dataFormat.getFormat("0.00%"));

        // Status-specific styles
        CellStyle okStyle = workbook.createCellStyle();
        okStyle.cloneStyleFrom(valueStyle);
        okStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        okStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle brokenStyle = workbook.createCellStyle();
        brokenStyle.cloneStyleFrom(valueStyle);
        brokenStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        brokenStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle redirectStyle = workbook.createCellStyle();
        redirectStyle.cloneStyleFrom(valueStyle);
        redirectStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        redirectStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle skippedStyle = workbook.createCellStyle();
        skippedStyle.cloneStyleFrom(valueStyle);
        skippedStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        skippedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle errorStyle = workbook.createCellStyle();
        errorStyle.cloneStyleFrom(valueStyle);
        errorStyle.setFillForegroundColor(IndexedColors.CORAL.getIndex());
        errorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // ---------------------------
        // Metrics
        // ---------------------------
        long total = results.size();

        long totalLinks = results.stream()
                .filter(r -> "LINK".equalsIgnoreCase(r.getResourceType()))
                .count();

        long totalImages = results.stream()
                .filter(r -> "IMAGE".equalsIgnoreCase(r.getResourceType()))
                .count();

        long ok = results.stream()
                .filter(r -> "OK".equalsIgnoreCase(r.getStatus()))
                .count();

        long broken = results.stream()
                .filter(r -> "BROKEN".equalsIgnoreCase(r.getStatus()))
                .count();

        long redirect = results.stream()
                .filter(r -> "REDIRECT".equalsIgnoreCase(r.getStatus()))
                .count();

        long skipped = results.stream()
                .filter(r -> "SKIPPED".equalsIgnoreCase(r.getStatus()))
                .count();

        long error = results.stream()
                .filter(r -> "ERROR".equalsIgnoreCase(r.getStatus()))
                .count();

        long okLinks = results.stream()
                .filter(r -> "LINK".equalsIgnoreCase(r.getResourceType())
                        && "OK".equalsIgnoreCase(r.getStatus()))
                .count();

        long okImages = results.stream()
                .filter(r -> "IMAGE".equalsIgnoreCase(r.getResourceType())
                        && "OK".equalsIgnoreCase(r.getStatus()))
                .count();

        long brokenLinks = results.stream()
                .filter(r -> "LINK".equalsIgnoreCase(r.getResourceType())
                        && "BROKEN".equalsIgnoreCase(r.getStatus()))
                .count();

        long brokenImages = results.stream()
                .filter(r -> "IMAGE".equalsIgnoreCase(r.getResourceType())
                        && "BROKEN".equalsIgnoreCase(r.getStatus()))
                .count();

        double okPct = total == 0 ? 0 : (double) ok / total;
        double brokenPct = total == 0 ? 0 : (double) broken / total;
        double redirectPct = total == 0 ? 0 : (double) redirect / total;
        double skippedPct = total == 0 ? 0 : (double) skipped / total;
        double errorPct = total == 0 ? 0 : (double) error / total;

        // ---------------------------
        // Sheet Layout
        // ---------------------------
        int rowIdx = 0;

        // Title
        Row titleRow = sheet.createRow(rowIdx++);
        titleRow.setHeightInPoints(24);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Broken Link Checker - Executive Summary Report");
        titleCell.setCellStyle(titleStyle);

        // Merge title across columns
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
        for (int i = 1; i <= 2; i++) {
            Cell mergedCell = titleRow.createCell(i);
            mergedCell.setCellStyle(titleStyle);
        }

        rowIdx++;

        // Report metadata section
        rowIdx = writeSectionHeader(sheet, rowIdx, "Report Information", sectionHeaderStyle);
        rowIdx = writeKeyValueRow(sheet, rowIdx, "Report Generated On",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), labelStyle, valueStyle);
        rowIdx = writeKeyValueRow(sheet, rowIdx, "Report Type",
                "Broken Link Validation Summary", labelStyle, valueStyle);
        rowIdx = writeKeyValueRow(sheet, rowIdx, "Total Resources Checked",
                total, labelStyle, valueStyle);

        rowIdx++;

        // Resource summary
        rowIdx = writeSectionHeader(sheet, rowIdx, "Resource Breakdown", sectionHeaderStyle);
        rowIdx = writeKeyValueRow(sheet, rowIdx, "Total Links Checked", totalLinks, labelStyle, valueStyle);
        rowIdx = writeKeyValueRow(sheet, rowIdx, "Total Images Checked", totalImages, labelStyle, valueStyle);
        rowIdx = writeKeyValueRow(sheet, rowIdx, "Healthy Links", okLinks, labelStyle, okStyle);
        rowIdx = writeKeyValueRow(sheet, rowIdx, "Healthy Images", okImages, labelStyle, okStyle);
        rowIdx = writeKeyValueRow(sheet, rowIdx, "Broken Links", brokenLinks, labelStyle, brokenStyle);
        rowIdx = writeKeyValueRow(sheet, rowIdx, "Broken Images", brokenImages, labelStyle, brokenStyle);

        rowIdx++;

        // Status summary
        rowIdx = writeSectionHeader(sheet, rowIdx, "Status Overview", sectionHeaderStyle);
        rowIdx = writeMetricRow(sheet, rowIdx, "OK", ok, okPct, labelStyle, okStyle, percentStyle);
        rowIdx = writeMetricRow(sheet, rowIdx, "Broken", broken, brokenPct, labelStyle, brokenStyle, percentStyle);
        rowIdx = writeMetricRow(sheet, rowIdx, "Redirect", redirect, redirectPct, labelStyle, redirectStyle, percentStyle);
        rowIdx = writeMetricRow(sheet, rowIdx, "Skipped", skipped, skippedPct, labelStyle, skippedStyle, percentStyle);
        rowIdx = writeMetricRow(sheet, rowIdx, "Error / Unreachable", error, errorPct, labelStyle, errorStyle, percentStyle);

        rowIdx++;

        // Observations section
        rowIdx = writeSectionHeader(sheet, rowIdx, "Summary Insight", sectionHeaderStyle);
        String overallRemark;
        if (broken == 0 && error == 0) {
            overallRemark = "All checked resources are healthy. No broken or unreachable resources were found.";
        } else if (broken > 0 || error > 0) {
            overallRemark = "Some resources require attention. Review broken and unreachable items in the detailed report.";
        } else {
            overallRemark = "Validation completed successfully.";
        }
        rowIdx = writeKeyValueRow(sheet, rowIdx, "Overall Assessment", overallRemark, labelStyle, valueStyle);

        // Freeze top rows
        sheet.createFreezePane(0, 2);

        // Auto size columns
        for (int i = 0; i <= 2; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 1200);
        }
    }

    private static int writeSectionHeader(Sheet sheet, int rowIdx, String title, CellStyle style) {
        Row row = sheet.createRow(rowIdx++);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(style);

        sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, 2));
        for (int i = 1; i <= 2; i++) {
            Cell mergedCell = row.createCell(i);
            mergedCell.setCellStyle(style);
        }
        return rowIdx;
    }

    private static int writeKeyValueRow(Sheet sheet, int rowIdx, String label, Object value,
                                        CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowIdx++);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        Cell valueCell = row.createCell(1);
        if (value instanceof Number) {
            valueCell.setCellValue(((Number) value).doubleValue());
        } else {
            valueCell.setCellValue(String.valueOf(value));
        }
        valueCell.setCellStyle(valueStyle);

        Cell emptyCell = row.createCell(2);
        emptyCell.setCellStyle(valueStyle);

        return rowIdx;
    }

    private static int writeMetricRow(Sheet sheet, int rowIdx, String label, long count, double percentage,
                                      CellStyle labelStyle, CellStyle valueStyle, CellStyle percentStyle) {
        Row row = sheet.createRow(rowIdx++);

        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);

        Cell countCell = row.createCell(1);
        countCell.setCellValue(count);
        countCell.setCellStyle(valueStyle);

        Cell pctCell = row.createCell(2);
        pctCell.setCellValue(percentage);
        pctCell.setCellStyle(percentStyle);

        return rowIdx;
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
