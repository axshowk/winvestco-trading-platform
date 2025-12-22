package in.winvestco.common.enums;

/**
 * Status of a report in its lifecycle
 */
public enum ReportStatus {
    PENDING, // Queued for processing
    PROCESSING, // Currently generating
    COMPLETED, // Ready for download
    FAILED, // Generation failed
    EXPIRED // Report file deleted (cleanup)
}
