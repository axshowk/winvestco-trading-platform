package in.winvestco.newssentimentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sentiment scores for each category.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentScores {
    private Double positive;
    private Double negative;
    private Double neutral;
}
