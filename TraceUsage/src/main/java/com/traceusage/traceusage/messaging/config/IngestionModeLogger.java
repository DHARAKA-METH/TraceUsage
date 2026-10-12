package com.traceusage.traceusage.messaging.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class IngestionModeLogger implements ApplicationRunner {

    private final IngestionProperties ingestionProperties;

    public IngestionModeLogger(IngestionProperties ingestionProperties) {
        this.ingestionProperties = ingestionProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("=============================================");
        System.out.println("TRACEUSAGE INGESTION MODE -> " + ingestionProperties.getMode());
        if (ingestionProperties.isKafkaMode()) {
            System.out.println("Telemetry flow: Collector -> Kafka -> Consumer -> PostgreSQL");
        } else {
            System.out.println("Telemetry flow: Collector -> PostgreSQL");
        }
        System.out.println("=============================================");
    }
}
