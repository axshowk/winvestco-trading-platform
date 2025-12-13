package in.winvestco.order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled service to expire DAY orders at EOD
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderExpiryScheduler {

    private final OrderService orderService;

    @Value("${trading.market-close-hour:15}")
    private int marketCloseHour;

    /**
     * Run expiry check every minute during market hours
     */
    @Scheduled(cron = "0 * * * * *")
    public void checkExpiredOrders() {
        try {
            int expiredCount = orderService.expireOrders();
            if (expiredCount > 0) {
                log.info("Expired {} orders", expiredCount);
            }
        } catch (Exception e) {
            log.error("Error expiring orders", e);
        }
    }

    /**
     * Run expiry check at market close (15:30 IST)
     */
    @Scheduled(cron = "0 30 15 * * MON-FRI", zone = "Asia/Kolkata")
    public void expireOrdersAtMarketClose() {
        log.info("Market close: Running order expiry check");
        try {
            int expiredCount = orderService.expireOrders();
            log.info("Market close: Expired {} orders", expiredCount);
        } catch (Exception e) {
            log.error("Error during market close expiry", e);
        }
    }
}
