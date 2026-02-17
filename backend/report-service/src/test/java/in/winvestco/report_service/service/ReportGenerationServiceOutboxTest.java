package in.winvestco.report_service.service;

import in.winvestco.common.event.ReportCompletedEvent;
import in.winvestco.common.messaging.outbox.OutboxService;
import in.winvestco.report_service.model.Report;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReportGenerationServiceOutboxTest {

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private ReportGenerationService reportGenerationService;

    @Test
    void shouldCaptureReportCompletedEventInOutbox() {
        // Given
        Report report = Report.builder()
                .reportId("RPT-001")
                .userId(123L)
                .completedAt(Instant.now())
                .build();

        // When
        reportGenerationService.publishCompletionEvent(report);

        // Then
        verify(outboxService).captureEvent(
                eq("Report"),
                eq("RPT-001"),
                eq("notification.exchange"),
                eq("report.completed"),
                any(ReportCompletedEvent.class)
        );
    }
}
