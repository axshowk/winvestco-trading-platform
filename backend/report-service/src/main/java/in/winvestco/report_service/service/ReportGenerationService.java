package in.winvestco.report_service.service;

import in.winvestco.common.enums.ReportFormat;
import in.winvestco.common.event.ReportCompletedEvent;
import in.winvestco.common.event.ReportFailedEvent;
import in.winvestco.common.messaging.outbox.OutboxService;
import in.winvestco.report_service.dto.PnLReportData;
import in.winvestco.report_service.dto.TaxReportData;
import in.winvestco.report_service.generator.CsvReportGenerator;
import in.winvestco.report_service.generator.ExcelReportGenerator;
import in.winvestco.report_service.generator.PdfReportGenerator;
import in.winvestco.report_service.model.Report;
import in.winvestco.report_service.model.projection.HoldingProjection;
import in.winvestco.report_service.model.projection.LedgerProjection;
import in.winvestco.report_service.model.projection.TradeProjection;
import in.winvestco.report_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static in.winvestco.common.config.RabbitMQConfig.*;

/**
 * Service responsible for generating reports asynchronously.
 * Reads from local projection tables (Event Sourcing pattern).
 * Uses outbox pattern for event publishing to ensure transactional safety.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGenerationService {

    private final ReportRepository reportRepository;
    private final TradeProjectionRepository tradeProjectionRepository;
    private final HoldingProjectionRepository holdingProjectionRepository;
    private final LedgerProjectionRepository ledgerProjectionRepository;

    private final PdfReportGenerator pdfGenerator;
    private final ExcelReportGenerator excelGenerator;
    private final CsvReportGenerator csvGenerator;

    private final OutboxService outboxService;

    @Value("${report.storage.path:./reports}")
    private String storagePath;

    /**
     * Generate report asynchronously
     */
    @Async
    @Transactional
    public void generateReportAsync(Long reportId) {
        log.info("Starting async report generation for ID: {}", reportId);

        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        try {
            report.startProcessing();
            reportRepository.save(report);

            // Generate based on report type
            byte[] content = switch (report.getReportType()) {
                case P_AND_L -> generatePnLReport(report);
                case TAX_REPORT -> generateTaxReport(report);
                case TRANSACTION_HISTORY -> generateTransactionHistoryReport(report);
                case HOLDINGS_SUMMARY -> generateHoldingsSummaryReport(report);
                case TRADE_HISTORY -> generateTradeHistoryReport(report);
            };

            // Save file
            String fileName = buildFileName(report);
            Path filePath = saveReportFile(content, fileName);

            // Update report status
            report.complete(filePath.toString(), (long) content.length);
            reportRepository.save(report);

            // Publish completion event
            publishCompletionEvent(report);

            log.info("Report {} generated successfully: {}", report.getReportId(), fileName);

        } catch (Exception e) {
            log.error("Failed to generate report {}: {}", report.getReportId(), e.getMessage(), e);
            report.fail(e.getMessage());
            reportRepository.save(report);
            publishFailureEvent(report, e.getMessage());
        }
    }

    private byte[] generatePnLReport(Report report) {
        Long userId = report.getUserId();
        Instant fromDate = report.getFromDate() != null ? report.getFromDate()
                : Instant.now().minus(365, ChronoUnit.DAYS);
        Instant toDate = report.getToDate() != null ? report.getToDate() : Instant.now();

        // Get trades for realized P&L
        List<TradeProjection> trades = tradeProjectionRepository
                .findByUserIdAndDateRange(userId, fromDate, toDate);

        // Get holdings for unrealized P&L
        List<HoldingProjection> holdings = holdingProjectionRepository.findByUserId(userId);

        // Calculate P&L data
        PnLReportData data = buildPnLReportData(userId, trades, holdings, fromDate, toDate);

        return generateInFormat(data, report.getFormat(), "P&L Report");
    }

    private byte[] generateTaxReport(Report report) {
        Long userId = report.getUserId();
        Instant fromDate = report.getFromDate() != null ? report.getFromDate()
                : getFinancialYearStart();
        Instant toDate = report.getToDate() != null ? report.getToDate() : Instant.now();

        List<TradeProjection> trades = tradeProjectionRepository
                .findByUserIdAndDateRange(userId, fromDate, toDate);

        TaxReportData data = buildTaxReportData(userId, trades, fromDate, toDate);

        return generateInFormat(data, report.getFormat(), "Tax Report");
    }

    private byte[] generateTransactionHistoryReport(Report report) {
        Long userId = report.getUserId();
        Instant fromDate = report.getFromDate() != null ? report.getFromDate()
                : Instant.now().minus(90, ChronoUnit.DAYS);
        Instant toDate = report.getToDate() != null ? report.getToDate() : Instant.now();

        List<LedgerProjection> ledger = ledgerProjectionRepository
                .findByUserIdAndDateRange(userId, fromDate, toDate);

        return generateInFormat(ledger, report.getFormat(), "Transaction History");
    }

    private byte[] generateHoldingsSummaryReport(Report report) {
        List<HoldingProjection> holdings = holdingProjectionRepository
                .findByUserId(report.getUserId());

        return generateInFormat(holdings, report.getFormat(), "Holdings Summary");
    }

    private byte[] generateTradeHistoryReport(Report report) {
        Long userId = report.getUserId();
        Instant fromDate = report.getFromDate() != null ? report.getFromDate()
                : Instant.now().minus(365, ChronoUnit.DAYS);
        Instant toDate = report.getToDate() != null ? report.getToDate() : Instant.now();

        List<TradeProjection> trades = tradeProjectionRepository
                .findByUserIdAndDateRange(userId, fromDate, toDate);

        return generateInFormat(trades, report.getFormat(), "Trade History");
    }

    private byte[] generateInFormat(Object data, ReportFormat format, String title) {
        return switch (format) {
            case PDF -> pdfGenerator.generate(data, title);
            case EXCEL -> excelGenerator.generate(data, title);
            case CSV -> csvGenerator.generate(data, title);
        };
    }

    private PnLReportData buildPnLReportData(Long userId, List<TradeProjection> trades,
            List<HoldingProjection> holdings,
            Instant fromDate, Instant toDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault());

        // Calculate realized P&L from trades (simplified - actual would need buy/sell
        // matching)
        BigDecimal realizedPnL = BigDecimal.ZERO;
        List<PnLReportData.TradePnL> tradePnLs = new ArrayList<>();

        for (TradeProjection trade : trades) {
            if (trade.isSell()) {
                // Simplified: assuming profit = sell price - some average (mock)
                BigDecimal profit = trade.getValue().multiply(new BigDecimal("0.05")); // Mock 5% profit
                realizedPnL = realizedPnL.add(profit);

                tradePnLs.add(PnLReportData.TradePnL.builder()
                        .tradeId(trade.getTradeId())
                        .symbol(trade.getSymbol())
                        .side(trade.getSide())
                        .quantity(trade.getQuantity())
                        .sellPrice(trade.getPrice())
                        .realizedPnL(profit)
                        .executedAt(formatter.format(trade.getExecutedAt()))
                        .build());
            }
        }

        // Calculate unrealized P&L from holdings (mock current prices)
        BigDecimal unrealizedPnL = BigDecimal.ZERO;
        List<PnLReportData.HoldingPnL> holdingPnLs = new ArrayList<>();

        for (HoldingProjection holding : holdings) {
            if (holding.getQuantity().compareTo(BigDecimal.ZERO) > 0) {
                // Mock current price as 5% above average (for demo)
                BigDecimal currentPrice = holding.getAveragePrice().multiply(new BigDecimal("1.05"));
                BigDecimal currentValue = holding.getQuantity().multiply(currentPrice);
                BigDecimal pnl = currentValue.subtract(holding.getTotalInvested());
                BigDecimal pnlPercent = holding.getTotalInvested().compareTo(BigDecimal.ZERO) > 0
                        ? pnl.divide(holding.getTotalInvested(), 4, RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100"))
                        : BigDecimal.ZERO;

                unrealizedPnL = unrealizedPnL.add(pnl);

                holdingPnLs.add(PnLReportData.HoldingPnL.builder()
                        .symbol(holding.getSymbol())
                        .quantity(holding.getQuantity())
                        .averagePrice(holding.getAveragePrice())
                        .currentPrice(currentPrice)
                        .investedValue(holding.getTotalInvested())
                        .currentValue(currentValue)
                        .unrealizedPnL(pnl)
                        .unrealizedPnLPercent(pnlPercent)
                        .build());
            }
        }

        return PnLReportData.builder()
                .userId(userId)
                .fromDate(formatter.format(fromDate))
                .toDate(formatter.format(toDate))
                .generatedAt(formatter.format(Instant.now()))
                .totalRealizedPnL(realizedPnL)
                .totalUnrealizedPnL(unrealizedPnL)
                .totalPnL(realizedPnL.add(unrealizedPnL))
                .holdings(holdingPnLs)
                .realizedTrades(tradePnLs)
                .build();
    }

    private TaxReportData buildTaxReportData(Long userId, List<TradeProjection> trades,
            Instant fromDate, Instant toDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.systemDefault());

        // Group by STCG (< 365 days) and LTCG (>= 365 days)
        BigDecimal stcg = BigDecimal.ZERO;
        BigDecimal ltcg = BigDecimal.ZERO;
        List<TaxReportData.CapitalGainEntry> stcgEntries = new ArrayList<>();
        List<TaxReportData.CapitalGainEntry> ltcgEntries = new ArrayList<>();

        for (TradeProjection trade : trades) {
            if (trade.isSell()) {
                // Mock holding period (would need actual buy date in production)
                int holdingDays = (int) (Math.random() * 500); // Mock
                BigDecimal gain = trade.getValue().multiply(new BigDecimal("0.05")); // Mock 5% gain

                TaxReportData.CapitalGainEntry entry = TaxReportData.CapitalGainEntry.builder()
                        .symbol(trade.getSymbol())
                        .quantity(trade.getQuantity())
                        .sellDate(formatter.format(trade.getExecutedAt()))
                        .sellPrice(trade.getPrice())
                        .holdingDays(holdingDays)
                        .capitalGain(gain)
                        .gainType(holdingDays < 365 ? "STCG" : "LTCG")
                        .build();

                if (holdingDays < 365) {
                    stcg = stcg.add(gain);
                    stcgEntries.add(entry);
                } else {
                    ltcg = ltcg.add(gain);
                    ltcgEntries.add(entry);
                }
            }
        }

        String fy = getFinancialYearString();

        return TaxReportData.builder()
                .userId(userId)
                .financialYear(fy)
                .fromDate(formatter.format(fromDate))
                .toDate(formatter.format(toDate))
                .generatedAt(formatter.format(Instant.now()))
                .shortTermCapitalGains(stcg)
                .longTermCapitalGains(ltcg)
                .totalCapitalGains(stcg.add(ltcg))
                .stcgEntries(stcgEntries)
                .ltcgEntries(ltcgEntries)
                .build();
    }

    private String buildFileName(Report report) {
        String extension = switch (report.getFormat()) {
            case PDF -> ".pdf";
            case EXCEL -> ".xlsx";
            case CSV -> ".csv";
        };

        String typeName = report.getReportType().name().toLowerCase().replace("_", "-");
        return String.format("%s_%s_%s%s",
                typeName,
                report.getUserId(),
                report.getReportId().substring(0, 8),
                extension);
    }

    private Path saveReportFile(byte[] content, String fileName) throws IOException {
        Path dir = Paths.get(storagePath);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        Path filePath = dir.resolve(fileName);
        Files.write(filePath, content);
        return filePath;
    }

    private void publishCompletionEvent(Report report) {
        try {
            ReportCompletedEvent event = ReportCompletedEvent.builder()
                    .reportId(report.getReportId())
                    .userId(report.getUserId())
                    .reportType(report.getReportType())
                    .format(report.getFormat())
                    .fileSizeBytes(report.getFileSizeBytes())
                    .completedAt(report.getCompletedAt())
                    .build();

            log.info("Capturing ReportCompletedEvent in outbox for report: {}", report.getReportId());
            outboxService.captureEvent("Report", report.getReportId().toString(),
                    NOTIFICATION_EXCHANGE, "report.completed", event);
        } catch (Exception e) {
            log.warn("Failed to capture report completion event in outbox: {}", e.getMessage());
        }
    }

    private void publishFailureEvent(Report report, String reason) {
        try {
            ReportFailedEvent event = ReportFailedEvent.builder()
                    .reportId(report.getReportId())
                    .userId(report.getUserId())
                    .reportType(report.getReportType())
                    .reason(reason)
                    .failedAt(Instant.now())
                    .build();

            log.info("Capturing ReportFailedEvent in outbox for report: {}", report.getReportId());
            outboxService.captureEvent("Report", report.getReportId().toString(),
                    NOTIFICATION_EXCHANGE, "report.failed", event);
        } catch (Exception e) {
            log.warn("Failed to capture report failure event in outbox: {}", e.getMessage());
        }
    }

    private Instant getFinancialYearStart() {
        LocalDate now = LocalDate.now();
        int year = now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1;
        return LocalDate.of(year, 4, 1).atStartOfDay(ZoneId.systemDefault()).toInstant();
    }

    private String getFinancialYearString() {
        LocalDate now = LocalDate.now();
        int startYear = now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1;
        int endYear = startYear + 1;
        return String.format("%d-%02d", startYear, endYear % 100);
    }
}
