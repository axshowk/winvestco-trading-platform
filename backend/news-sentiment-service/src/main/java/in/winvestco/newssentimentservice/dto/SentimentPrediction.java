package in.winvestco.newssentimentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for sentiment prediction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentPrediction {
    private String text;
    private String sentiment;
    private Double confidence;
    private SentimentScores scores;
}
