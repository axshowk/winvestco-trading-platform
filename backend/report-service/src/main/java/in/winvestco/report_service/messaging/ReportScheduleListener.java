package in.winvestco.report_service.messaging;

import in.winvestco.common.config.RabbitMQConfig;
import in.winvestco.report_service.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportScheduleListener {

    private final ReportService reportService;

    @RabbitListener(queues = RabbitMQConfig.REPORT_CLEANUP_TRIGGER_QUEUE)
    public void handleReportCleanupTrigger(String message) {
        log.info("Received report cleanup trigger: {}", message);
        try {
            reportService.cleanupExpiredReports();
            log.info("Successfully completed expired reports cleanup");
        } catch (Exception e) {
            log.error("Error during triggered report cleanup", e);
        }
    }
}
