package com.traceusage.traceusage.messaging.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceusage.traceusage.fieldusage.service.FieldUsagePersistenceService;
import com.traceusage.traceusage.messaging.dto.FieldTelemetryBatchMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "traceusage.ingestion", name = "mode", havingValue = "kafka")
public class FieldEventConsumer {

    private final ObjectMapper objectMapper;
    private final FieldUsagePersistenceService persistenceService;

    public FieldEventConsumer(ObjectMapper objectMapper,
                              FieldUsagePersistenceService persistenceService) {
        this.objectMapper = objectMapper;
        this.persistenceService = persistenceService;
    }

    @KafkaListener(
            topics = "${traceusage.kafka.topics.field-events}",
            groupId = "traceusage-field-consumers")
    public void consume(String message) throws Exception {
        FieldTelemetryBatchMessage event = objectMapper.readValue(
                message,
                FieldTelemetryBatchMessage.class);

        persistenceService.saveIfNew(event);
        System.out.println("KAFKA FIELD BATCH SAVED -> batchId=" + event.batchId());
    }
}
