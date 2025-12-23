package in.winvestco.common.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

class LoggingUtilsTest {

    private LoggingUtils loggingUtils;

    @BeforeEach
    void setUp() {
        loggingUtils = new LoggingUtils();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void generateRequestId_ShouldSetMdcAndReturnId() {
        String requestId = loggingUtils.generateRequestId();
        assertNotNull(requestId);
        assertEquals(requestId, MDC.get("requestId"));
    }

    @Test
    void setUserId_ShouldSetMdc() {
        loggingUtils.setUserId("user-123");
        assertEquals("user-123", MDC.get("userId"));
    }

    @Test
    void setServiceName_ShouldSetMdc() {
        loggingUtils.setServiceName("test-service");
        assertEquals("test-service", MDC.get("serviceName"));
    }

    @Test
    void clearContext_ShouldClearMdc() {
        MDC.put("test", "value");
        loggingUtils.clearContext();
        assertNull(MDC.get("test"));
    }

    @Test
    void logPerformance_ShouldNotThrowException() {
        // Since logPerformance only logs and doesn't return value or change state
        // (other than MDC which it doesn't in this method),
        // we just ensure it doesn't crash for different durations.
        assertDoesNotThrow(() -> loggingUtils.logPerformance("testOp", 0, 500));
        assertDoesNotThrow(() -> loggingUtils.logPerformance("testOp", 0, 1500));
        assertDoesNotThrow(() -> loggingUtils.logPerformance("testOp", 0, 6000));
        assertDoesNotThrow(() -> loggingUtils.logPerformance("testOp", 0, 11000));
    }
}
