package in.winvestco.report_service.service;

import in.winvestco.common.enums.ReportStatus;
import in.winvestco.common.enums.ReportType;
import in.winvestco.common.enums.ReportFormat;
import in.winvestco.report_service.dto.ReportDTO;
import in.winvestco.report_service.dto.ReportRequest;
import in.winvestco.report_service.mapper.ReportMapper;
import in.winvestco.report_service.model.Report;
import in.winvestco.report_service.repository.ReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportMapper reportMapper;

    @Mock
    private ReportGenerationService reportGenerationService;

    @InjectMocks
    private ReportService reportService;

    private Report testReport;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reportService, "storagePath", "./reports");
        ReflectionTestUtils.setField(reportService, "maxRetentionDays", 30);

        testReport = Report.builder()
                .id(1L)
                .reportId("REP-123")
                .userId(1L)
                .reportType(ReportType.TRANSACTION_HISTORY)
                .format(ReportFormat.PDF)
                .status(ReportStatus.PENDING)
                .build();
    }

    @Test
    void requestReport_ShouldSaveAndTriggerGeneration() {
        ReportRequest request = new ReportRequest();
        request.setType(ReportType.TRANSACTION_HISTORY);
        request.setFormat(ReportFormat.PDF);
        request.setFromDate(Instant.now().minus(30, ChronoUnit.DAYS));
        request.setToDate(Instant.now());

        when(reportRepository.save(any(Report.class))).thenReturn(testReport);
        when(reportMapper.toDTO(any(Report.class))).thenReturn(new ReportDTO());

        ReportDTO result = reportService.requestReport(1L, request);

        assertNotNull(result);
        verify(reportRepository).save(any(Report.class));
        verify(reportGenerationService).generateReportAsync(anyLong());
    }

    @Test
    void getReport_ShouldReturnDTOWithDownloadUrl() {
        testReport.setStatus(ReportStatus.COMPLETED);
        testReport.setFilePath("./reports/REP-123.pdf");
        when(reportRepository.findByReportId(anyString())).thenReturn(Optional.of(testReport));

        ReportDTO dto = new ReportDTO();
        dto.setReportId("REP-123");
        when(reportMapper.toDTO(any(Report.class))).thenReturn(dto);

        ReportDTO result = reportService.getReport("REP-123");

        assertNotNull(result);
        assertTrue(result.getDownloadUrl().contains("/download"));
    }

    @Test
    void deleteReport_WhenAuthorized_ShouldDelete() {
        when(reportRepository.findByReportId(anyString())).thenReturn(Optional.of(testReport));

        reportService.deleteReport("REP-123", 1L);

        verify(reportRepository).delete(testReport);
    }

    @Test
    void deleteReport_WhenNotAuthorized_ShouldThrowException() {
        when(reportRepository.findByReportId(anyString())).thenReturn(Optional.of(testReport));

        assertThrows(RuntimeException.class, () -> reportService.deleteReport("REP-123", 2L));
    }
}
