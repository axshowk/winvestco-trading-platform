package in.winvestco.report_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for Tax Report data (Capital Gains)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxReportData {
    private Long userId;
    private String financialYear; // e.g., "2024-25"
    private String fromDate;
    private String toDate;
    private String generatedAt;
    
    // Summary
    private BigDecimal shortTermCapitalGains; // STCG (< 1 year)
    private BigDecimal longTermCapitalGains;  // LTCG (>= 1 year)
    private BigDecimal totalCapitalGains;
    
    // STCG details
    private List<CapitalGainEntry> stcgEntries;
    
    // LTCG details
    private List<CapitalGainEntry> ltcgEntries;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CapitalGainEntry {
        private String symbol;
        private BigDecimal quantity;
        private String buyDate;
        private BigDecimal buyPrice;
        private String sellDate;
        private BigDecimal sellPrice;
        private int holdingDays;
        private BigDecimal capitalGain;
        private String gainType; // STCG or LTCG
    }
}
