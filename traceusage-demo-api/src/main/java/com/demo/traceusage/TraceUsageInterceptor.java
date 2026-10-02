package com.demo.traceusage;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.time.Instant;
import java.util.UUID;

@Component
public class TraceUsageInterceptor implements HandlerInterceptor {

    private static final String START_ATTRIBUTE = "traceusage.start";

    private final TelemetryPublisher publisher;

    public TraceUsageInterceptor(TelemetryPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        request.setAttribute(START_ATTRIBUTE, System.nanoTime());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception exception) {
        try {
            String endpoint = (String) request.getAttribute(
                    HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);

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
