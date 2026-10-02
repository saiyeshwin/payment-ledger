package com.paymentledger.expense.integration;

import com.paymentledger.expense.entity.OutboxEvent;
import com.paymentledger.expense.repository.OutboxEventRepository;
import com.paymentledger.expense.service.OutboxPublisherService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Testcontainers
@SpringBootTest
@Tag("integration")
public class OutboxPublisherIntegrationTest {

    @Autowired
    private OutboxPublisherService outboxPublisherService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KafkaListenerEndpointRegistry registry;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("expense_db")
            .withUsername("postgres")
            .withPassword("postgres");

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeAll
    static void checkDocker() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is not available, skipping tests");
    }

    @Test
    void publishPendingEvents_whenOutboxHasUnpublishedEvent_publishesToKafka() {
        // Create manual event
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("EXPENSE");
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("EXPENSE_CREATED");
        event.setPayload("{\"test\":\"data\"}");
        event.setCreatedAt(OffsetDateTime.now());
        event.setPublished(false);
        
        event = outboxEventRepository.save(event);

        // Act
        outboxPublisherService.publishPendingEvents();

        // Assert
        OutboxEvent updatedEvent = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertTrue(updatedEvent.isPublished());

        var pending = outboxEventRepository.findUnpublishedEvents();
        assertFalse(pending.contains(updatedEvent));
    }
}
