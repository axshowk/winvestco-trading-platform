package in.winvestco.portfolio_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO for stock market data from Market Service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockQuoteDTO {
    private String symbol;
    private BigDecimal lastPrice;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal change;
    private BigDecimal pChange;
    private BigDecimal volume;
    private Instant lastUpdateTime;
}
