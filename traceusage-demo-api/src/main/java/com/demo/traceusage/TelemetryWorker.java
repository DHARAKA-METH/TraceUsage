package com.demo.traceusage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class TelemetryWorker implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(TelemetryWorker.class);

    private final TelemetryQueue queue;
    private final TelemetryHttpClient httpClient;
    private final TraceUsageProperties properties;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "traceusage-telemetry-worker");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean running;

    public TelemetryWorker(TelemetryQueue queue,
                           TelemetryHttpClient httpClient,
                           TraceUsageProperties properties) {
        this.queue = queue;
        this.httpClient = httpClient;
        this.properties = properties;
    }

    @Override
    public void start() {
        if (!properties.isEnabled()) {
            return;
        }
        running = true;
        executor.submit(this::processEvents);
    }

    @Override
    public void stop() {
        running = false;
        executor.shutdownNow();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void processEvents() {
        while (running) {
            try {
                UsageEvent first = queue.poll(5, TimeUnit.SECONDS);
                if (first == null) {
                    continue;
                }

                List<UsageEvent> batch = new ArrayList<>();
                batch.add(first);

                int batchSize = Math.max(1, Math.min(properties.getBatchSize(), 100));
                queue.drainTo(batch, batchSize - batch.size());

                sendWithBoundedRetry(batch);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception exception) {
                log.warn("TraceUsage telemetry worker ignored unexpected failure", exception);
            }
        }
        running = false;
    }

    private void sendWithBoundedRetry(List<UsageEvent> batch) {
        int maxRetries = Math.max(0, properties.getMaxRetries());
        int maxAttempts = maxRetries + 1;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                httpClient.sendBatch(batch);
                return;
            } catch (RestClientResponseException exception) {
                if (!shouldRetryStatus(exception.getStatusCode().value())) {
                    log.warn("TraceUsage telemetry batch dropped after HTTP {}", exception.getStatusCode().value());
                    return;
                }
                sleepBeforeRetry(attempt, maxAttempts);
            } catch (Exception exception) {
                sleepBeforeRetry(attempt, maxAttempts);
            }
        }

        log.warn("TraceUsage telemetry batch dropped after {} attempts", maxAttempts);
    }

    private boolean shouldRetryStatus(int statusCode) {
        return statusCode == HttpStatus.TOO_MANY_REQUESTS.value()
                || statusCode == HttpStatus.SERVICE_UNAVAILABLE.value()
                || statusCode >= 500;
    }

    private void sleepBeforeRetry(int attempt, int maxAttempts) {
        if (attempt >= maxAttempts) {
            return;
        }

        try {
            Thread.sleep(Math.max(0, properties.getRetryDelayMillis()) * attempt);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }
}
