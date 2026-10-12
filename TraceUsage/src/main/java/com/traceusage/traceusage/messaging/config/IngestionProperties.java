package com.traceusage.traceusage.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "traceusage.ingestion")
public class IngestionProperties {

    private String mode = "direct";

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isKafkaMode() {
        return "kafka".equalsIgnoreCase(mode);
    }
}
