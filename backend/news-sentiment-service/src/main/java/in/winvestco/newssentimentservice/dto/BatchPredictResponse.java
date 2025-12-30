package in.winvestco.newssentimentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for batch sentiment prediction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPredictResponse {
    private List<SentimentPrediction> predictions;
    private Integer total;
}
