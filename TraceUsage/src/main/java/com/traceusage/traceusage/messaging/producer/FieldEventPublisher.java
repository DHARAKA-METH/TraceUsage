package com.traceusage.traceusage.messaging.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceusage.traceusage.messaging.config.KafkaTopicProperties;
import com.traceusage.traceusage.messaging.dto.FieldTelemetryBatchMessage;
import com.traceusage.traceusage.messaging.exception.KafkaIngestionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@ConditionalOnProperty(prefix = "traceusage.ingestion", name = "mode", havingValue = "kafka")
public class FieldEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicProperties topics;

    public FieldEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               KafkaTopicProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topics = topics;
    }

    public CompletableFuture<SendResult<String, String>> publish(FieldTelemetryBatchMessage batch) {
        try {
            String value = objectMapper.writeValueAsString(batch);
            String key = batch.applicationId() + "|" + batch.clientId() + "|" + batch.batchId();
            return kafkaTemplate.send(topics.getFieldEvents(), key, value);
        } catch (JsonProcessingException exception) {
            CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
            failed.completeExceptionally(new KafkaIngestionException(
                    "Failed to serialize field telemetry batch", exception));
            return failed;
        }
    }
}
