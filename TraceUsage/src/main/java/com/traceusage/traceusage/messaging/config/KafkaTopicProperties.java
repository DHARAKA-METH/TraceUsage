package com.traceusage.traceusage.messaging.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "traceusage.kafka.topics")
public class KafkaTopicProperties {

    private String endpointEvents = "traceusage.endpoint-events";
    private String fieldEvents = "traceusage.field-events";
    private String endpointEventsDlt = "traceusage.endpoint-events.dlt";
    private String fieldEventsDlt = "traceusage.field-events.dlt";

    public String getEndpointEvents() {
        return endpointEvents;
    }

    public void setEndpointEvents(String endpointEvents) {
        this.endpointEvents = endpointEvents;
    }

    public String getFieldEvents() {
        return fieldEvents;
    }

    public void setFieldEvents(String fieldEvents) {
        this.fieldEvents = fieldEvents;
    }

    public String getEndpointEventsDlt() {
        return endpointEventsDlt;
    }

    public void setEndpointEventsDlt(String endpointEventsDlt) {
        this.endpointEventsDlt = endpointEventsDlt;
    }

    public String getFieldEventsDlt() {
        return fieldEventsDlt;
    }

    public void setFieldEventsDlt(String fieldEventsDlt) {
        this.fieldEventsDlt = fieldEventsDlt;
    }
}
