package in.winvestco.marketservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for candle data in API responses.
 * Uses Unix timestamp for frontend compatibility with Lightweight Charts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandleDTO {
    private Long time; // Unix timestamp in seconds (for Lightweight Charts)
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private Long volume;

    /**
     * Create CandleDTO from entity with time conversion.
     */
    public static CandleDTO fromEntity(in.winvestco.marketservice.entity.Candle candle) {
        return CandleDTO.builder()
                .time(candle.getTimestamp().atZone(java.time.ZoneId.of("Asia/Kolkata")).toEpochSecond())
                .open(candle.getOpen())
                .high(candle.getHigh())
                .low(candle.getLow())
                .close(candle.getClose())
                .volume(candle.getVolume())
                .build();
    }
}
