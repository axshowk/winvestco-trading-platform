package in.winvestco.report_service.generator;

import in.winvestco.report_service.dto.PnLReportData;
import in.winvestco.report_service.dto.TaxReportData;
import in.winvestco.report_service.model.projection.HoldingProjection;
import in.winvestco.report_service.model.projection.LedgerProjection;
import in.winvestco.report_service.model.projection.TradeProjection;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Excel Report Generator using Apache POI
 */
@Component
@Slf4j
public class ExcelReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    @SuppressWarnings("unchecked")
    public byte[] generate(Object data, String title) {
        try (Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Generate based on data type
            if (data instanceof PnLReportData) {
                generatePnLReport(workbook, (PnLReportData) data, headerStyle);
            } else if (data instanceof TaxReportData) {
                generateTaxReport(workbook, (TaxReportData) data, headerStyle);
            } else if (data instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof TradeProjection) {
                    generateTradeHistory(workbook, (List<TradeProjection>) data, headerStyle);
                } else if (first instanceof HoldingProjection) {
                    generateHoldingsSummary(workbook, (List<HoldingProjection>) data, headerStyle);
                } else if (first instanceof LedgerProjection) {
                    generateTransactionHistory(workbook, (List<LedgerProjection>) data, headerStyle);
                }
            }

            workbook.write(baos);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate Excel report", e);
            throw new RuntimeException("Excel generation failed", e);
        }
    }

    private void generatePnLReport(Workbook workbook, PnLReportData data, CellStyle headerStyle) {
        // Summary sheet
        Sheet summarySheet = workbook.createSheet("Summary");
        int row = 0;

        addRow(summarySheet, row++, "P&L Report");
        addRow(summarySheet, row++, "Period", data.getFromDate() + " to " + data.getToDate());
        addRow(summarySheet, row++, "Generated", data.getGeneratedAt());
        row++;
        addRow(summarySheet, row++, "Realized P&L", "₹" + data.getTotalRealizedPnL());
        addRow(summarySheet, row++, "Unrealized P&L", "₹" + data.getTotalUnrealizedPnL());
        addRow(summarySheet, row++, "Total P&L", "₹" + data.getTotalPnL());

        // Holdings sheet
        if (data.getHoldings() != null && !data.getHoldings().isEmpty()) {
            Sheet holdingsSheet = workbook.createSheet("Holdings");
            String[] headers = { "Symbol", "Quantity", "Avg Price", "Current Price", "P&L", "P&L %" };
            createHeaderRow(holdingsSheet, headers, headerStyle);

            int r = 1;
            for (var h : data.getHoldings()) {
                Row dataRow = holdingsSheet.createRow(r++);
                dataRow.createCell(0).setCellValue(h.getSymbol());
                dataRow.createCell(1).setCellValue(h.getQuantity().doubleValue());
                dataRow.createCell(2).setCellValue(h.getAveragePrice().doubleValue());
                dataRow.createCell(3).setCellValue(h.getCurrentPrice().doubleValue());
                dataRow.createCell(4).setCellValue(h.getUnrealizedPnL().doubleValue());
                dataRow.createCell(5).setCellValue(h.getUnrealizedPnLPercent().doubleValue() + "%");
            }
            autoSizeColumns(holdingsSheet, 6);
        }
    }

    private void generateTaxReport(Workbook workbook, TaxReportData data, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Tax Report");
        int row = 0;

        addRow(sheet, row++, "Capital Gains Tax Report");
        addRow(sheet, row++, "Financial Year", data.getFinancialYear());
        addRow(sheet, row++, "Generated", data.getGeneratedAt());
        row++;
        addRow(sheet, row++, "Short-Term Capital Gains", "₹" + data.getShortTermCapitalGains());
        addRow(sheet, row++, "Long-Term Capital Gains", "₹" + data.getLongTermCapitalGains());
        addRow(sheet, row++, "Total Capital Gains", "₹" + data.getTotalCapitalGains());
    }

    private void generateTradeHistory(Workbook workbook, List<TradeProjection> trades, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Trade History");
        String[] headers = { "Symbol", "Side", "Quantity", "Price", "Value", "Date" };
        createHeaderRow(sheet, headers, headerStyle);

        int row = 1;
        for (TradeProjection trade : trades) {
            Row dataRow = sheet.createRow(row++);
            dataRow.createCell(0).setCellValue(trade.getSymbol());
            dataRow.createCell(1).setCellValue(trade.getSide());
            dataRow.createCell(2).setCellValue(trade.getQuantity().doubleValue());
            dataRow.createCell(3).setCellValue(trade.getPrice().doubleValue());
            dataRow.createCell(4).setCellValue(trade.getValue().doubleValue());
            dataRow.createCell(5).setCellValue(DATE_FORMATTER.format(trade.getExecutedAt()));
        }
        autoSizeColumns(sheet, 6);
    }

    private void generateHoldingsSummary(Workbook workbook, List<HoldingProjection> holdings, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Holdings");
        String[] headers = { "Symbol", "Quantity", "Avg Price", "Total Invested" };
        createHeaderRow(sheet, headers, headerStyle);

        int row = 1;
        for (HoldingProjection h : holdings) {
            Row dataRow = sheet.createRow(row++);
            dataRow.createCell(0).setCellValue(h.getSymbol());
            dataRow.createCell(1).setCellValue(h.getQuantity().doubleValue());
            dataRow.createCell(2).setCellValue(h.getAveragePrice().doubleValue());
            dataRow.createCell(3).setCellValue(h.getTotalInvested().doubleValue());
        }
        autoSizeColumns(sheet, 4);
    }

    private void generateTransactionHistory(Workbook workbook, List<LedgerProjection> ledger, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Transactions");
        String[] headers = { "Date", "Type", "Amount", "Balance Before", "Balance After", "Description" };
        createHeaderRow(sheet, headers, headerStyle);

        int row = 1;
        for (LedgerProjection l : ledger) {
            Row dataRow = sheet.createRow(row++);
            dataRow.createCell(0).setCellValue(DATE_FORMATTER.format(l.getCreatedAt()));
            dataRow.createCell(1).setCellValue(l.getEntryType());
            dataRow.createCell(2).setCellValue(l.getAmount().doubleValue());
            dataRow.createCell(3).setCellValue(l.getBalanceBefore().doubleValue());
            dataRow.createCell(4).setCellValue(l.getBalanceAfter().doubleValue());
            dataRow.createCell(5).setCellValue(l.getDescription() != null ? l.getDescription() : "");
        }
        autoSizeColumns(sheet, 6);
    }

    private void createHeaderRow(Sheet sheet, String[] headers, CellStyle style) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void addRow(Sheet sheet, int rowNum, String... values) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private void autoSizeColumns(Sheet sheet, int numColumns) {
        for (int i = 0; i < numColumns; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}
