package in.winvestco.common.event;

import in.winvestco.common.enums.ReportType;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;

/**
 * Event published when a report generation fails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportFailedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private String reportId;
    private Long userId;
    private ReportType reportType;
    private String reason;
    private Instant failedAt;
}
