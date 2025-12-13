package in.winvestco.portfolio_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for trade operations (buy/sell)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponse {

    private boolean success;
    private String message;
    private HoldingDTO holding;

    public static TradeResponse success(String message, HoldingDTO holding) {
        return TradeResponse.builder()
                .success(true)
                .message(message)
                .holding(holding)
                .build();
    }

    public static TradeResponse error(String message) {
        return TradeResponse.builder()
                .success(false)
                .message(message)
                .holding(null)
                .build();
    }
}
