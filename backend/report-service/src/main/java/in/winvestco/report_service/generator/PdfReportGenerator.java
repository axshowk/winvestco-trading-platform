package in.winvestco.report_service.generator;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import in.winvestco.report_service.dto.PnLReportData;
import in.winvestco.report_service.dto.TaxReportData;
import in.winvestco.report_service.model.projection.HoldingProjection;
import in.winvestco.report_service.model.projection.LedgerProjection;
import in.winvestco.report_service.model.projection.TradeProjection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PDF Report Generator using iText
 */
@Component
@Slf4j
public class PdfReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    public byte[] generate(Object data, String title) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Add title
            document.add(new Paragraph(title)
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20));

            // Generate content based on data type
            if (data instanceof PnLReportData) {
                generatePnLReport(document, (PnLReportData) data);
            } else if (data instanceof TaxReportData) {
                generateTaxReport(document, (TaxReportData) data);
            } else if (data instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof TradeProjection) {
                    generateTradeHistory(document, (List<TradeProjection>) data);
                } else if (first instanceof HoldingProjection) {
                    generateHoldingsSummary(document, (List<HoldingProjection>) data);
                } else if (first instanceof LedgerProjection) {
                    generateTransactionHistory(document, (List<LedgerProjection>) data);
                }
            }

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate PDF report", e);
            throw new RuntimeException("PDF generation failed", e);
        }
    }

    private void generatePnLReport(Document doc, PnLReportData data) {
        // Summary section
        doc.add(new Paragraph("Report Period: " + data.getFromDate() + " to " + data.getToDate()));
        doc.add(new Paragraph("Generated: " + data.getGeneratedAt()));
        doc.add(new Paragraph("").setMarginBottom(10));

        // P&L Summary
        doc.add(new Paragraph("P&L Summary").setBold().setFontSize(14));
        doc.add(new Paragraph("Realized P&L: ₹" + data.getTotalRealizedPnL()));
        doc.add(new Paragraph("Unrealized P&L: ₹" + data.getTotalUnrealizedPnL()));
        doc.add(new Paragraph("Total P&L: ₹" + data.getTotalPnL()).setBold());
        doc.add(new Paragraph("").setMarginBottom(15));

        // Holdings table
        if (data.getHoldings() != null && !data.getHoldings().isEmpty()) {
            doc.add(new Paragraph("Current Holdings").setBold().setFontSize(14));
            Table table = new Table(UnitValue.createPercentArray(6)).useAllAvailableWidth();
            table.addHeaderCell(new Cell().add(new Paragraph("Symbol").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Qty").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Avg Price").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Current Price").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("P&L").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("P&L %").setBold()));

            for (var h : data.getHoldings()) {
                table.addCell(h.getSymbol());
                table.addCell(h.getQuantity().toString());
                table.addCell("₹" + h.getAveragePrice());
                table.addCell("₹" + h.getCurrentPrice());
                table.addCell("₹" + h.getUnrealizedPnL());
                table.addCell(h.getUnrealizedPnLPercent() + "%");
            }
            doc.add(table);
        }
    }

    private void generateTaxReport(Document doc, TaxReportData data) {
        doc.add(new Paragraph("Financial Year: " + data.getFinancialYear()));
        doc.add(new Paragraph("Generated: " + data.getGeneratedAt()));
        doc.add(new Paragraph("").setMarginBottom(10));

        // Tax Summary
        doc.add(new Paragraph("Capital Gains Summary").setBold().setFontSize(14));
        doc.add(new Paragraph("Short-Term Capital Gains (STCG): ₹" + data.getShortTermCapitalGains()));
        doc.add(new Paragraph("Long-Term Capital Gains (LTCG): ₹" + data.getLongTermCapitalGains()));
        doc.add(new Paragraph("Total Capital Gains: ₹" + data.getTotalCapitalGains()).setBold());
    }

    private void generateTradeHistory(Document doc, List<TradeProjection> trades) {
        Table table = new Table(UnitValue.createPercentArray(5)).useAllAvailableWidth();
        table.addHeaderCell(new Cell().add(new Paragraph("Symbol").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Side").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Qty").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Price").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()));

        for (TradeProjection trade : trades) {
            table.addCell(trade.getSymbol());
            table.addCell(trade.getSide());
            table.addCell(trade.getQuantity().toString());
            table.addCell("₹" + trade.getPrice());
            table.addCell(DATE_FORMATTER.format(trade.getExecutedAt()));
        }
        doc.add(table);
    }

    private void generateHoldingsSummary(Document doc, List<HoldingProjection> holdings) {
        Table table = new Table(UnitValue.createPercentArray(4)).useAllAvailableWidth();
        table.addHeaderCell(new Cell().add(new Paragraph("Symbol").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Quantity").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Avg Price").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Total Invested").setBold()));

        for (HoldingProjection h : holdings) {
            table.addCell(h.getSymbol());
            table.addCell(h.getQuantity().toString());
            table.addCell("₹" + h.getAveragePrice());
            table.addCell("₹" + h.getTotalInvested());
        }
        doc.add(table);
    }

    private void generateTransactionHistory(Document doc, List<LedgerProjection> ledger) {
        Table table = new Table(UnitValue.createPercentArray(5)).useAllAvailableWidth();
        table.addHeaderCell(new Cell().add(new Paragraph("Date").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Type").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Amount").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Balance").setBold()));
        table.addHeaderCell(new Cell().add(new Paragraph("Description").setBold()));

        for (LedgerProjection l : ledger) {
            table.addCell(DATE_FORMATTER.format(l.getCreatedAt()));
            table.addCell(l.getEntryType());
            table.addCell("₹" + l.getAmount());
            table.addCell("₹" + l.getBalanceAfter());
            table.addCell(l.getDescription() != null ? l.getDescription() : "");
        }
        doc.add(table);
    }
}
