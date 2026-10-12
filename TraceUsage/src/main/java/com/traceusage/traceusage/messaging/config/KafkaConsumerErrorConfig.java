package com.traceusage.traceusage.messaging.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "traceusage.ingestion", name = "mode", havingValue = "kafka")
public class KafkaConsumerErrorConfig {

    @Bean
    DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaTopicProperties topics) {
        return new DeadLetterPublishingRecoverer(kafkaTemplate, (record, exception) -> {
            String sourceTopic = record.topic();
            String dltTopic = sourceTopic.equals(topics.getFieldEvents())
                    ? topics.getFieldEventsDlt()
                    : topics.getEndpointEventsDlt();
            return new org.apache.kafka.common.TopicPartition(dltTopic, record.partition());
        });
    }

    @Bean
    DefaultErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        ExponentialBackOff backOff = new ExponentialBackOff(1_000L, 2.0);
        backOff.setMaxElapsedTime(7_000L);
        return new DefaultErrorHandler(recoverer, backOff);
    }
}
