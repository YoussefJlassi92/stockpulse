package com.stockpulse.portfolio.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic portfolioUpdatesTopic(@Value("${app.kafka.topics.portfolio-updates}") String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(5)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic portfolioDlqTopic() {
        return TopicBuilder.name("portfolio.dlq")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
