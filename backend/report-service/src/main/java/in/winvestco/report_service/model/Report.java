package in.winvestco.report_service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import in.winvestco.common.enums.ReportFormat;
import in.winvestco.common.enums.ReportStatus;
import in.winvestco.common.enums.ReportType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;

/**
 * Report entity representing a generated or pending report.
 * Tracks the full lifecycle from request to completion/failure.
 */
@Entity
@Table(name = "reports", indexes = {
    @Index(name = "idx_reports_user_id", columnList = "user_id"),
    @Index(name = "idx_reports_status", columnList = "status"),
    @Index(name = "idx_reports_requested_at", columnList = "requested_at"),
    @Index(name = "idx_reports_type_user", columnList = "report_type, user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Report implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @Column(name = "report_id", nullable = false, unique = true, length = 36)
    private String reportId;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 30)
    private ReportType reportType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 10)
    private ReportFormat format;

    @Column(name = "from_date")
    private Instant fromDate;

    @Column(name = "to_date")
    private Instant toDate;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    // ==================== Lifecycle Methods ====================

    /**
     * Mark report as processing
     */
    public void startProcessing() {
        this.status = ReportStatus.PROCESSING;
    }

    /**
     * Mark report as completed with file info
     */
    public void complete(String filePath, Long fileSizeBytes) {
        this.status = ReportStatus.COMPLETED;
        this.filePath = filePath;
        this.fileSizeBytes = fileSizeBytes;
        this.completedAt = Instant.now();
    }

    /**
     * Mark report as failed
     */
    public void fail(String errorMessage) {
        this.status = ReportStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = Instant.now();
    }

    /**
     * Check if report is downloadable
     */
    public boolean isDownloadable() {
        return status == ReportStatus.COMPLETED && filePath != null;
    }
}
