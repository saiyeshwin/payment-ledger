package com.paymentledger.ledger.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class KafkaTopicConfig {

    @Bean
    public NewTopic expenseCreatedTopic() {
        return TopicBuilder.name("expense.created")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic expenseCreatedDltTopic() {
        return TopicBuilder.name("expense.created.DLT")
                .partitions(1)
                .replicas(1)
                .build();
    }
}
