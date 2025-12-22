package in.winvestco.report_service.dto;

import in.winvestco.common.enums.ReportFormat;
import in.winvestco.common.enums.ReportType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

/**
 * Request DTO for creating a new report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {
    
    @NotNull(message = "Report type is required")
    private ReportType type;
    
    @NotNull(message = "Report format is required")
    private ReportFormat format;
    
    private Instant fromDate;
    
    private Instant toDate;
}
