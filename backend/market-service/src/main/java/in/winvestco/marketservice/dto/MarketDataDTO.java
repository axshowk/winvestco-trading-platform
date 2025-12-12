package in.winvestco.marketservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketDataDTO {

    private String symbol;
    private String exchange;
    private String tradingSymbol;

    @JsonProperty("ltp")
    private BigDecimal lastTradedPrice;

    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;

    private Long volume;

    @JsonProperty("change")
    private BigDecimal changeValue;

    @JsonProperty("changePercent")
    private BigDecimal changePercentage;

    private LocalDateTime timestamp;

    // Additional fields from Angel One
    private BigDecimal upperCircuitLimit;
    private BigDecimal lowerCircuitLimit;
    private Long totalBuyQuantity;
    private Long totalSellQuantity;
}
