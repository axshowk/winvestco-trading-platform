package in.winvestco.report_service.generator;

import com.opencsv.CSVWriter;
import in.winvestco.report_service.dto.PnLReportData;
import in.winvestco.report_service.dto.TaxReportData;
import in.winvestco.report_service.model.projection.HoldingProjection;
import in.winvestco.report_service.model.projection.LedgerProjection;
import in.winvestco.report_service.model.projection.TradeProjection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CSV Report Generator using OpenCSV
 */
@Component
@Slf4j
public class CsvReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    @SuppressWarnings("unchecked")
    public byte[] generate(Object data, String title) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
                CSVWriter writer = new CSVWriter(osw)) {

            // Generate based on data type
            if (data instanceof PnLReportData) {
                generatePnLReport(writer, (PnLReportData) data);
            } else if (data instanceof TaxReportData) {
                generateTaxReport(writer, (TaxReportData) data);
            } else if (data instanceof List<?> list && !list.isEmpty()) {
                Object first = list.get(0);
                if (first instanceof TradeProjection) {
                    generateTradeHistory(writer, (List<TradeProjection>) data);
                } else if (first instanceof HoldingProjection) {
                    generateHoldingsSummary(writer, (List<HoldingProjection>) data);
                } else if (first instanceof LedgerProjection) {
                    generateTransactionHistory(writer, (List<LedgerProjection>) data);
                }
            }

            writer.flush();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate CSV report", e);
            throw new RuntimeException("CSV generation failed", e);
        }
    }

    private void generatePnLReport(CSVWriter writer, PnLReportData data) {
        // Summary
        writer.writeNext(new String[] { "P&L Report" });
        writer.writeNext(new String[] { "Period", data.getFromDate() + " to " + data.getToDate() });
        writer.writeNext(new String[] { "Generated", data.getGeneratedAt() });
        writer.writeNext(new String[] {});
        writer.writeNext(new String[] { "Realized P&L", data.getTotalRealizedPnL().toString() });
        writer.writeNext(new String[] { "Unrealized P&L", data.getTotalUnrealizedPnL().toString() });
        writer.writeNext(new String[] { "Total P&L", data.getTotalPnL().toString() });
        writer.writeNext(new String[] {});

        // Holdings
        if (data.getHoldings() != null && !data.getHoldings().isEmpty()) {
            writer.writeNext(new String[] { "Symbol", "Quantity", "Avg Price", "Current Price", "P&L", "P&L %" });
            for (var h : data.getHoldings()) {
                writer.writeNext(new String[] {
                        h.getSymbol(),
                        h.getQuantity().toString(),
                        h.getAveragePrice().toString(),
                        h.getCurrentPrice().toString(),
                        h.getUnrealizedPnL().toString(),
                        h.getUnrealizedPnLPercent().toString()
                });
            }
        }
    }

    private void generateTaxReport(CSVWriter writer, TaxReportData data) {
        writer.writeNext(new String[] { "Capital Gains Tax Report" });
        writer.writeNext(new String[] { "Financial Year", data.getFinancialYear() });
        writer.writeNext(new String[] { "Generated", data.getGeneratedAt() });
        writer.writeNext(new String[] {});
        writer.writeNext(new String[] { "Short-Term Capital Gains", data.getShortTermCapitalGains().toString() });
        writer.writeNext(new String[] { "Long-Term Capital Gains", data.getLongTermCapitalGains().toString() });
        writer.writeNext(new String[] { "Total Capital Gains", data.getTotalCapitalGains().toString() });
    }

    private void generateTradeHistory(CSVWriter writer, List<TradeProjection> trades) {
        writer.writeNext(new String[] { "Symbol", "Side", "Quantity", "Price", "Value", "Date" });
        for (TradeProjection trade : trades) {
            writer.writeNext(new String[] {
                    trade.getSymbol(),
                    trade.getSide(),
                    trade.getQuantity().toString(),
                    trade.getPrice().toString(),
                    trade.getValue().toString(),
                    DATE_FORMATTER.format(trade.getExecutedAt())
            });
        }
    }

    private void generateHoldingsSummary(CSVWriter writer, List<HoldingProjection> holdings) {
        writer.writeNext(new String[] { "Symbol", "Quantity", "Avg Price", "Total Invested" });
        for (HoldingProjection h : holdings) {
            writer.writeNext(new String[] {
                    h.getSymbol(),
                    h.getQuantity().toString(),
                    h.getAveragePrice().toString(),
                    h.getTotalInvested().toString()
            });
        }
    }

    private void generateTransactionHistory(CSVWriter writer, List<LedgerProjection> ledger) {
        writer.writeNext(new String[] { "Date", "Type", "Amount", "Balance Before", "Balance After", "Description" });
        for (LedgerProjection l : ledger) {
            writer.writeNext(new String[] {
                    DATE_FORMATTER.format(l.getCreatedAt()),
                    l.getEntryType(),
                    l.getAmount().toString(),
                    l.getBalanceBefore().toString(),
                    l.getBalanceAfter().toString(),
                    l.getDescription() != null ? l.getDescription() : ""
            });
        }
    }
}
