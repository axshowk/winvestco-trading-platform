package in.winvestco.report_service.controller;

import in.winvestco.report_service.dto.ReportDTO;
import in.winvestco.report_service.dto.ReportRequest;
import in.winvestco.report_service.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for report management
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reports", description = "Report generation and download APIs")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "Request a new report")
    public ResponseEntity<ReportDTO> requestReport(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReportRequest request) {
        
        Long userId = Long.parseLong(jwt.getSubject());
        log.info("Report request from user {}: type={}", userId, request.getType());
        
        ReportDTO report = reportService.requestReport(userId, request);
        return ResponseEntity.accepted().body(report);
    }

    @GetMapping("/{reportId}")
    @Operation(summary = "Get report details")
    public ResponseEntity<ReportDTO> getReport(@PathVariable String reportId) {
        ReportDTO report = reportService.getReport(reportId);
        return ResponseEntity.ok(report);
    }

    @GetMapping
    @Operation(summary = "Get all reports for the authenticated user")
    public ResponseEntity<Page<ReportDTO>> getUserReports(
            @AuthenticationPrincipal Jwt jwt,
            Pageable pageable) {
        
        Long userId = Long.parseLong(jwt.getSubject());
        Page<ReportDTO> reports = reportService.getUserReports(userId, pageable);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/{reportId}/download")
    @Operation(summary = "Download a completed report")
    public ResponseEntity<Resource> downloadReport(@PathVariable String reportId) {
        Resource resource = reportService.downloadReport(reportId);
        
        String contentType = determineContentType(reportId);
        String filename = resource.getFilename() != null ? resource.getFilename() : "report";
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @DeleteMapping("/{reportId}")
    @Operation(summary = "Delete a report")
    public ResponseEntity<Void> deleteReport(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String reportId) {
        
        Long userId = Long.parseLong(jwt.getSubject());
        reportService.deleteReport(reportId, userId);
        return ResponseEntity.noContent().build();
    }

    private String determineContentType(String reportId) {
        ReportDTO report = reportService.getReport(reportId);
        return switch (report.getFormat()) {
            case PDF -> "application/pdf";
            case EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case CSV -> "text/csv";
        };
    }
}
