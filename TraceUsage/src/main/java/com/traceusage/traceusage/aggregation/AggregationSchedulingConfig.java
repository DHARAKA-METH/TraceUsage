package com.traceusage.traceusage.aggregation;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(TelemetryRetentionProperties.class)
public class AggregationSchedulingConfig {
}