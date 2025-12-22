package in.winvestco.common.event;

import in.winvestco.common.enums.ReportFormat;
import in.winvestco.common.enums.ReportType;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * Event published when a report is completed successfully
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCompletedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String reportId;
    private Long userId;
    private ReportType reportType;
    private ReportFormat format;
    private String fileName;
    private Long fileSizeBytes;
    private Instant completedAt;
}
