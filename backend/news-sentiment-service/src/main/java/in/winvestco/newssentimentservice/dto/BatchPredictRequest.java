package in.winvestco.newssentimentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for batch sentiment prediction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPredictRequest {
    private List<String> texts;
}
