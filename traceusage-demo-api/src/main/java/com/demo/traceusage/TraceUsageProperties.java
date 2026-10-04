package com.demo.traceusage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "traceusage")
public class TraceUsageProperties {

    private boolean enabled = true;

    private String endpoint = "http://localhost:8080/api/v1/events";

    private String batchEndpoint = "http://localhost:8080/api/v1/events/batch";

    private String apiKey = "";

    private int batchSize = 100;

    private int maxRetries = 3;

    private long retryDelayMillis = 1_000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBatchEndpoint() {
        return batchEndpoint;
    }

    public void setBatchEndpoint(String batchEndpoint) {
        this.batchEndpoint = batchEndpoint;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryDelayMillis() {
        return retryDelayMillis;
    }

    public void setRetryDelayMillis(long retryDelayMillis) {
        this.retryDelayMillis = retryDelayMillis;
    }
}
