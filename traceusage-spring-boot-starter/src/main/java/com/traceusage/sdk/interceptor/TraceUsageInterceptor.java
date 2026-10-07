package com.traceusage.sdk.interceptor;

import com.traceusage.sdk.service.TelemetryPublisher;
import com.traceusage.sdk.telemetry.UsageEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.time.Instant;
import java.util.UUID;

public class TraceUsageInterceptor implements HandlerInterceptor {

    private final TelemetryPublisher publisher;

    public TraceUsageInterceptor(TelemetryPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception exception) {
        try {
            String endpoint = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            if (endpoint == null || endpoint.isBlank()) {
                return;
            }

            UsageEvent event = new UsageEvent(
                    UUID.randomUUID(),
                    request.getMethod(),
                    endpoint,
                    response.getStatus(),
                    Instant.now());

            publisher.publish(event);
        } catch (Exception ignored) {
            // Telemetry must never fail the business request.
        }
    }
}
