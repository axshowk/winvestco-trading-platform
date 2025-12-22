package in.winvestco.report_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for P&L Report data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PnLReportData {
    private Long userId;
    private String fromDate;
    private String toDate;
    private String generatedAt;
    
    // Summary
    private BigDecimal totalRealizedPnL;
    private BigDecimal totalUnrealizedPnL;
    private BigDecimal totalPnL;
    
    // Holdings with unrealized P&L
    private List<HoldingPnL> holdings;
    
    // Realized trades
    private List<TradePnL> realizedTrades;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HoldingPnL {
        private String symbol;
        private BigDecimal quantity;
        private BigDecimal averagePrice;
        private BigDecimal currentPrice;
        private BigDecimal investedValue;
        private BigDecimal currentValue;
        private BigDecimal unrealizedPnL;
        private BigDecimal unrealizedPnLPercent;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradePnL {
        private String tradeId;
        private String symbol;
        private String side;
        private BigDecimal quantity;
        private BigDecimal buyPrice;
        private BigDecimal sellPrice;
        private BigDecimal realizedPnL;
        private String executedAt;
    }
}
