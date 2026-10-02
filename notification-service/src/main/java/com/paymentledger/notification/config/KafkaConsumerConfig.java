package com.paymentledger.notification.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Bean
    public DefaultErrorHandler errorHandler() {
        // Log and skip on exhaustion — no DLT for notification service
        ConsumerRecordRecoverer recoverer = (record, exception) ->
                log.error("Notification consumer: retries exhausted for record on topic={}, partition={}, offset={}. Error: {}",
                        record.topic(), record.partition(), record.offset(), exception.getMessage());

        ExponentialBackOff backOff = new ExponentialBackOff(500L, 2.0);
        backOff.setMaxElapsedTime(2000L); // ~2 retries at 500ms, 1000ms

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
