package in.winvestco.common.event;

import in.winvestco.common.enums.ReportType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Event published when a report generation fails
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ReportFailedEvent extends BaseEvent {
    private static final long serialVersionUID = 1L;

    private String reportId;
    private Long userId;
    private ReportType reportType;
    private String reason;
    private Instant failedAt;
}
