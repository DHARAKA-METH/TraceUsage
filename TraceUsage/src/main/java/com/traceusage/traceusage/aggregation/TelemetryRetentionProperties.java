package com.traceusage.traceusage.aggregation;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurable retention policy for raw telemetry, hourly aggregates and
 * idempotency (deduplication) records.
 *
 * <p>Defaults follow the recommended hybrid policy:
 * raw rows kept short-term for troubleshooting, aggregates kept long-term for
 * analytics, deduplication records kept longer than the Kafka replay window so
 * redelivered events can never inflate counts.
 */
@ConfigurationProperties(prefix = "traceusage.telemetry.retention")
public class TelemetryRetentionProperties {

    public enum StorageMode {
        /** Raw events only. Aggregates are still maintained but retention cleanup does not delete raw rows. */
        RAW_ONLY,
        /** Recent raw rows plus long-term hourly aggregates. Default. */
        HYBRID
    }

    private boolean enabled = true;

    private StorageMode storageMode = StorageMode.HYBRID;

    private int rawDays = 14;

    private int aggregateDays = 730;

    private int deduplicationDays = 30;

    private long cleanupIntervalMs = 3_600_000L;

    private int deleteBatchLimit = 50_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public StorageMode getStorageMode() {
        return storageMode;
    }

    public void setStorageMode(StorageMode storageMode) {
        this.storageMode = storageMode;
    }

    public int getRawDays() {
        return rawDays;
    }

    public void setRawDays(int rawDays) {
        this.rawDays = rawDays;
    }

    public int getAggregateDays() {
        return aggregateDays;
    }

    public void setAggregateDays(int aggregateDays) {
        this.aggregateDays = aggregateDays;
    }

    public int getDeduplicationDays() {
        return deduplicationDays;
    }

    public void setDeduplicationDays(int deduplicationDays) {
        this.deduplicationDays = deduplicationDays;
    }

    public long getCleanupIntervalMs() {
        return cleanupIntervalMs;
    }

    public void setCleanupIntervalMs(long cleanupIntervalMs) {
        this.cleanupIntervalMs = cleanupIntervalMs;
    }

    public int getDeleteBatchLimit() {
        return deleteBatchLimit;
    }

    public void setDeleteBatchLimit(int deleteBatchLimit) {
        this.deleteBatchLimit = deleteBatchLimit;
    }

    public boolean deletesRawEvents() {
        return enabled && storageMode == StorageMode.HYBRID;
    }
}