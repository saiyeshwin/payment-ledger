package com.paymentledger.expense.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentledger.expense.entity.OutboxEvent;
import com.paymentledger.expense.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OutboxPublisherService {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.expense-created:expense.created}")
    private String expenseCreatedTopic;

    @Value("${kafka.topics.expense-updated:expense.updated}")
    private String expenseUpdatedTopic;

    @Value("${kafka.topics.expense-deleted:expense.deleted}")
    private String expenseDeletedTopic;

    public OutboxPublisherService(OutboxEventRepository outboxEventRepository, KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:500}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> unpublishedEvents = outboxEventRepository.findUnpublishedEvents();

        for (OutboxEvent event : unpublishedEvents) {
            String topic = determineTopic(event.getEventType());
            if (topic != null) {
                try {
                    String key = event.getAggregateId().toString();
                    
                    kafkaTemplate.send(topic, key, event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                markEventAsPublished(event);
                            } else {
                                log.error("Failed to publish outbox event {}", event.getId(), ex);
                            }
                        });
                } catch (Exception e) {
                    log.error("Error processing outbox event {}", event.getId(), e);
                }
            } else {
                log.warn("Unknown event type {} for outbox event {}", event.getEventType(), event.getId());
            }
        }
    }

    private void markEventAsPublished(OutboxEvent event) {
        event.setPublished(true);
        event.setPublishedAt(OffsetDateTime.now());
        outboxEventRepository.save(event);
    }

    private String determineTopic(String eventType) {
        return switch (eventType) {
            case "EXPENSE_CREATED" -> expenseCreatedTopic;
            case "EXPENSE_UPDATED" -> expenseUpdatedTopic;
            case "EXPENSE_DELETED" -> expenseDeletedTopic;
            default -> null;
        };
    }
}
