package com.traceusage.traceusage.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceusage.traceusage.messaging.dto.EndpointTelemetryMessage;
import com.traceusage.traceusage.telemetry.service.EndpointTelemetryPersistenceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "traceusage.ingestion", name = "mode", havingValue = "kafka")
public class EndpointEventConsumer {

    private final ObjectMapper objectMapper;
    private final EndpointTelemetryPersistenceService persistenceService;

    public EndpointEventConsumer(ObjectMapper objectMapper,
                                 EndpointTelemetryPersistenceService persistenceService) {
        this.objectMapper = objectMapper;
        this.persistenceService = persistenceService;
    }

    @KafkaListener(
            topics = "${traceusage.kafka.topics.endpoint-events}",
            groupId = "traceusage-endpoint-consumers")
    public void consume(String message) throws Exception {
        EndpointTelemetryMessage event = objectMapper.readValue(
                message,
                EndpointTelemetryMessage.class);

        persistenceService.saveIfNew(event);
        System.out.println("KAFKA ENDPOINT EVENT SAVED -> eventId=" + event.eventId());
    }
}
