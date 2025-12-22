package in.winvestco.report_service.dto;

import in.winvestco.common.enums.ReportFormat;
import in.winvestco.common.enums.ReportStatus;
import in.winvestco.common.enums.ReportType;
import lombok.*;

import java.time.Instant;

/**
 * DTO for Report responses
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDTO {
    private String reportId;
    private Long userId;
    private ReportType reportType;
    private ReportStatus status;
    private ReportFormat format;
    private Instant fromDate;
    private Instant toDate;
    private Long fileSizeBytes;
    private String errorMessage;
    private Instant requestedAt;
    private Instant completedAt;
    private String downloadUrl;
}
