package in.winvestco.common.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.winvestco.common.service.RateLimitService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.lang.NonNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String clientIP = getClientIP(request);

        // Create rate limit key based on client IP and endpoint
        String rateLimitKey = String.format("api:%s:%s:%s", method.toLowerCase(), uri, clientIP);

        // Check rate limit
        boolean allowed = rateLimitService.isAllowed(rateLimitKey);

        if (!allowed) {
            log.warn("Rate limit exceeded for IP: {} on endpoint: {} {}", clientIP, method, uri);

            // Set rate limit headers
            setRateLimitHeaders(response, rateLimitKey);

            // Send rate limit exceeded response
            sendRateLimitResponse(response, rateLimitKey);
            return false;
        }

        // Set rate limit headers for successful requests
        setRateLimitHeaders(response, rateLimitKey);

        return true;
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }

        return request.getRemoteAddr();
    }

    private void setRateLimitHeaders(HttpServletResponse response, String rateLimitKey) {
        int remaining = rateLimitService.getRemainingRequests(rateLimitKey);
        long resetTime = rateLimitService.getResetTime(rateLimitKey);

        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetTime));
    }

    private void sendRateLimitResponse(HttpServletResponse response, String rateLimitKey) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Rate limit exceeded");
        errorResponse.put("message", "Too many requests. Please try again later.");
        errorResponse.put("remainingRequests", rateLimitService.getRemainingRequests(rateLimitKey));
        errorResponse.put("resetTimeInSeconds", rateLimitService.getResetTime(rateLimitKey));

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.getWriter().write(jsonResponse);
    }
}
