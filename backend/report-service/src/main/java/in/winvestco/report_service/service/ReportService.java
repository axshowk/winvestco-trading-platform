package in.winvestco.report_service.service;

import in.winvestco.common.enums.ReportStatus;
import in.winvestco.report_service.dto.ReportDTO;
import in.winvestco.report_service.dto.ReportRequest;
import in.winvestco.report_service.mapper.ReportMapper;
import in.winvestco.report_service.model.Report;
import in.winvestco.report_service.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Main service for report management.
 * Handles report requests, status checks, downloads, and cleanup.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;
    private final ReportGenerationService reportGenerationService;

    @Value("${report.storage.path:./reports}")
    private String storagePath;

    @Value("${report.storage.max-retention-days:30}")
    private int maxRetentionDays;

    /**
     * Request a new report for the user.
     * Creates a PENDING report and triggers async generation.
     */
    @Transactional
    public ReportDTO requestReport(Long userId, ReportRequest request) {
        log.info("Requesting report for user {}: type={}, format={}",
                userId, request.getType(), request.getFormat());

        // Create report entity
        Report report = Report.builder()
                .reportId(UUID.randomUUID().toString())
                .userId(userId)
                .reportType(request.getType())
                .format(request.getFormat())
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .status(ReportStatus.PENDING)
                .build();

        report = reportRepository.save(report);

        // Trigger async generation
        reportGenerationService.generateReportAsync(report.getId());

        log.info("Report {} queued for generation", report.getReportId());
        return reportMapper.toDTO(report);
    }

    /**
     * Get report by ID
     */
    @Transactional(readOnly = true)
    public ReportDTO getReport(String reportId) {
        Report report = reportRepository.findByReportId(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        ReportDTO dto = reportMapper.toDTO(report);
        if (report.isDownloadable()) {
            dto.setDownloadUrl("/api/reports/" + reportId + "/download");
        }
        return dto;
    }

    /**
     * Get all reports for a user
     */
    @Transactional(readOnly = true)
    public Page<ReportDTO> getUserReports(Long userId, Pageable pageable) {
        return reportRepository.findByUserIdOrderByRequestedAtDesc(userId, pageable)
                .map(report -> {
                    ReportDTO dto = reportMapper.toDTO(report);
                    if (report.isDownloadable()) {
                        dto.setDownloadUrl("/api/reports/" + report.getReportId() + "/download");
                    }
                    return dto;
                });
    }

    /**
     * Download a completed report
     */
    @Transactional(readOnly = true)
    public Resource downloadReport(String reportId) {
        Report report = reportRepository.findByReportId(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        if (!report.isDownloadable()) {
            throw new RuntimeException("Report is not available for download. Status: " + report.getStatus());
        }

        try {
            Path filePath = Paths.get(report.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Report file not found or not readable");
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading report file", e);
        }
    }

    /**
     * Delete a report
     */
    @Transactional
    public void deleteReport(String reportId, Long userId) {
        Report report = reportRepository.findByReportId(reportId)
                .orElseThrow(() -> new RuntimeException("Report not found: " + reportId));

        if (!report.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this report");
        }

        // Delete file if exists
        if (report.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(report.getFilePath()));
            } catch (IOException e) {
                log.warn("Failed to delete report file: {}", report.getFilePath(), e);
            }
        }

        reportRepository.delete(report);
        log.info("Deleted report: {}", reportId);
    }

    /**
     * Cleanup expired reports - triggered via RabbitMQ
     */
    @Transactional
    public void cleanupExpiredReports() {
        log.info("Starting expired reports cleanup");

        Instant expiryDate = Instant.now().minus(maxRetentionDays, ChronoUnit.DAYS);
        var expiredReports = reportRepository.findExpiredReports(expiryDate);

        int deletedCount = 0;
        for (Report report : expiredReports) {
            try {
                if (report.getFilePath() != null) {
                    Files.deleteIfExists(Paths.get(report.getFilePath()));
                }
                report.setStatus(ReportStatus.EXPIRED);
                report.setFilePath(null);
                reportRepository.save(report);
                deletedCount++;
            } catch (IOException e) {
                log.warn("Failed to cleanup report file: {}", report.getFilePath(), e);
            }
        }

        log.info("Cleanup completed: {} reports expired", deletedCount);
    }

    /**
     * Get storage path for reports
     */
    public Path getStoragePath() {
        Path path = Paths.get(storagePath);
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage directory", e);
        }
        return path;
    }
}
