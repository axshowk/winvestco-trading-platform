package in.winvestco.newssentimentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for single sentiment prediction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentPredictRequest {
    private String text;
}
