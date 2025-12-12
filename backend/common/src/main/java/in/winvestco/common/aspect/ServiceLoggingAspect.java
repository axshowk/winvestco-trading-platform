package in.winvestco.common.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import in.winvestco.common.util.LoggingUtils;

import java.util.Arrays;

/**
 * Aspect for automatic logging of service methods.
 * Provides consistent logging across all services in the application.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ServiceLoggingAspect {

    private final LoggingUtils loggingUtils;

    /**
     * Around advice for all service methods to provide automatic logging
     */
    @Around("execution(* com.binvestco..service.*Service.*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String serviceName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getMethod().getName();
        String operation = serviceName + "." + methodName;

        // Generate request ID for tracing
        loggingUtils.generateRequestId();

        long startTime = System.currentTimeMillis();

        try {
            // Log method entry
            Object[] args = joinPoint.getArgs();
            loggingUtils.logServiceStart(serviceName, methodName, Arrays.toString(args));

            // Execute the method
            Object result = joinPoint.proceed();

            // Log method completion
            long endTime = System.currentTimeMillis();
            loggingUtils.logServiceEnd(serviceName, methodName, result);
            loggingUtils.logPerformance(operation, startTime, endTime);

            return result;

        } catch (Exception ex) {
            // Log method failure
            long endTime = System.currentTimeMillis();
            loggingUtils.logError(serviceName, methodName, ex, joinPoint.getArgs());
            loggingUtils.logPerformance(operation + " (FAILED)", startTime, endTime);

            throw ex;
        } finally {
            // Clear MDC context
            loggingUtils.clearContext();
        }
    }

    /**
     * Around advice for controller methods to provide request logging
     */
    @Around("execution(* com.binvestco..controller.*Controller.*(..))")
    public Object logControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String controllerName = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getMethod().getName();
        String operation = controllerName + "." + methodName;

        // Generate request ID for tracing
        loggingUtils.generateRequestId();

        long startTime = System.currentTimeMillis();

        try {
            // Log controller method entry
            Object[] args = joinPoint.getArgs();
            log.debug("Controller method called: {} with args: {}", operation, Arrays.toString(args));

            // Execute the method
            Object result = joinPoint.proceed();

            // Log controller method completion
            long endTime = System.currentTimeMillis();
            log.debug("Controller method completed: {} in {} ms", operation, (endTime - startTime));

            return result;

        } catch (Exception ex) {
            // Log controller method failure
            long endTime = System.currentTimeMillis();
            log.error("Controller method failed: {} after {} ms - Error: {}",
                     operation, (endTime - startTime), ex.getMessage(), ex);

            throw ex;
        } finally {
            // Clear MDC context
            loggingUtils.clearContext();
        }
    }
}
