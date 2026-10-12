package com.traceusage.traceusage.messaging.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "traceusage.ingestion", name = "mode", havingValue = "kafka")
public class KafkaTopicConfig {

    @Bean
    NewTopic endpointEventsTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.getEndpointEvents())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic fieldEventsTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.getFieldEvents())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic endpointEventsDltTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.getEndpointEventsDlt())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic fieldEventsDltTopic(KafkaTopicProperties topics) {
        return TopicBuilder.name(topics.getFieldEventsDlt())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
