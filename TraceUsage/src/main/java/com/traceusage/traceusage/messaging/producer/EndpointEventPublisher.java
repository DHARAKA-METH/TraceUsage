package com.traceusage.traceusage.messaging.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.traceusage.traceusage.messaging.config.KafkaTopicProperties;
import com.traceusage.traceusage.messaging.dto.EndpointTelemetryMessage;
import com.traceusage.traceusage.messaging.exception.KafkaIngestionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@ConditionalOnProperty(prefix = "traceusage.ingestion", name = "mode", havingValue = "kafka")
public class EndpointEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaTopicProperties topics;

    public EndpointEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                  ObjectMapper objectMapper,
                                  KafkaTopicProperties topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topics = topics;
    }

    public CompletableFuture<SendResult<String, String>> publish(EndpointTelemetryMessage event) {
        try {
            String value = objectMapper.writeValueAsString(event);
            String key = event.applicationId() + "|" + event.endpoint();
            return kafkaTemplate.send(topics.getEndpointEvents(), key, value);
        } catch (JsonProcessingException exception) {
            CompletableFuture<SendResult<String, String>> failed = new CompletableFuture<>();
            failed.completeExceptionally(new KafkaIngestionException(
                    "Failed to serialize endpoint telemetry event", exception));
            return failed;
        }
    }
}
