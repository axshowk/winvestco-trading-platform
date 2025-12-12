package in.winvestco.common.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import in.winvestco.common.annotation.Auditable;

import java.time.Instant;
import java.util.Arrays;

/**
 * Aspect for audit logging of methods annotated with @Auditable.
 * Logs important business operations to a separate audit log file.
 */
@Slf4j
@Aspect
@Component
public class AuditLoggingAspect {

    private static final String AUDIT_LOGGER = "AUDIT";

    /**
     * Around advice for methods annotated with @Auditable
     */
    @Around("@annotation(auditable)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String serviceName = joinPoint.getTarget().getClass().getSimpleName();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getMethod().getName();
        String requestId = MDC.get("requestId");
        String userId = MDC.get("userId");

        // Prepare audit information
        String action = auditable.action();
        String context = auditable.context();
        String targetInfo = buildTargetInfo(joinPoint.getArgs());

        long startTime = System.currentTimeMillis();

        try {
            // Execute the method
            Object result = joinPoint.proceed();

            // Log successful audit event
            logAuditEvent(AUDIT_LOGGER, "SUCCESS", action, serviceName, methodName,
                         requestId != null ? requestId : "N/A", userId != null ? userId : "N/A", context, targetInfo, null, startTime);

            return result;

        } catch (Exception ex) {
            // Log failed audit event
            logAuditEvent(AUDIT_LOGGER, "FAILED", action, serviceName, methodName,
                         requestId != null ? requestId : "N/A", userId != null ? userId : "N/A", context, targetInfo, ex.getMessage(),
                         System.currentTimeMillis());

            throw ex;
        }
    }

    private void logAuditEvent(String loggerName, String status, String action,
                              String serviceName, String methodName, String requestId,
                              String userId, String context, String targetInfo,
                              String errorMessage, long timestamp) {

        String auditMessage = String.format(
            "Action: %s | Status: %s | Service: %s | Method: %s | RequestId: %s | UserId: %s | Context: %s | Target: %s | Error: %s | Timestamp: %s",
            action, status, serviceName, methodName, requestId, userId, context, targetInfo,
            errorMessage != null ? errorMessage : "N/A", Instant.now().toString()
        );

        // Use the AUDIT logger which writes to the audit file
        org.slf4j.Logger auditLogger = org.slf4j.LoggerFactory.getLogger(AUDIT_LOGGER);
        auditLogger.info(auditMessage);
    }

    private String buildTargetInfo(Object[] args) {
        if (args == null || args.length == 0) {
            return "N/A";
        }

        return Arrays.toString(args);
    }
}
