package in.winvestco.common.event;

import in.winvestco.common.enums.ReportFormat;
import in.winvestco.common.enums.ReportType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Event published when a report is completed successfully
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ReportCompletedEvent extends BaseEvent {
    private static final long serialVersionUID = 1L;

    private String reportId;
    private Long userId;
    private ReportType reportType;
    private ReportFormat format;
    private String fileName;
    private Long fileSizeBytes;
    private Instant completedAt;
}
