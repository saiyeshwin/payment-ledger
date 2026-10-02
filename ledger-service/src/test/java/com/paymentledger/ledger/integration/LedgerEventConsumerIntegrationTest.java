package com.paymentledger.ledger.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentledger.ledger.entity.LedgerEntry;
import com.paymentledger.ledger.entity.ProcessedEventId;
import com.paymentledger.ledger.repository.LedgerEntryRepository;
import com.paymentledger.ledger.repository.ProcessedEventRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@SpringBootTest
@Testcontainers
@Tag("integration")
public class LedgerEventConsumerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("ledger_db")
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
        registry.add("kafka.topics.expense-created", () -> "expense.created");
        registry.add("kafka.topics.expense-created-dlt", () -> "expense.created.DLT");
    }

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void checkDockerAndClean() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker is not available - skipping integration test");
        ledgerEntryRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void testEndToEndEventFlow_WithDuplicateDeduplication() throws Exception {
        UUID expenseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("75.50");

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("expenseId", expenseId.toString());
        payloadMap.put("userId", userId.toString());
        payloadMap.put("amount", amount.toString());
        payloadMap.put("currency", "USD");
        payloadMap.put("category", "FOOD");
        payloadMap.put("status", "COMPLETED");

        String jsonPayload = objectMapper.writeValueAsString(payloadMap);

        // 1. Publish initial event to Kafka
        kafkaTemplate.send("expense.created", userId.toString(), jsonPayload).get(10, TimeUnit.SECONDS);

        // 2. Verify Ledger entry and ProcessedEvent created
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<LedgerEntry> entries = ledgerEntryRepository.findByExpenseId(expenseId);
            assertEquals(1, entries.size(), "Ledger entry should be recorded");
            assertEquals(0, amount.compareTo(entries.get(0).getAmount()), "Amount should match");
            assertEquals("DEBIT", entries.get(0).getTransactionType());

            boolean processed = processedEventRepository.existsById(new ProcessedEventId(expenseId, "ledger-service"));
            assertTrue(processed, "Event should be marked in processed_events");
        });

        // 3. Publish DUPLICATE event to Kafka
        kafkaTemplate.send("expense.created", userId.toString(), jsonPayload).get(10, TimeUnit.SECONDS);

        // Wait brief period and verify duplicate was safely ignored
        Thread.sleep(2000);

        List<LedgerEntry> entriesAfterDuplicate = ledgerEntryRepository.findByExpenseId(expenseId);
        assertEquals(1, entriesAfterDuplicate.size(), "Duplicate message must NOT create a second ledger entry");
    }

    @Test
    void testPoisonPillMessage_RoutesToDeadLetterTopic() throws Exception {
        // Send a poison message with malformed JSON / non-UUID payload that causes consumer error
        String poisonPayload = "{\"expenseId\": \"not-a-valid-uuid\", \"userId\": \"bad-user\", \"amount\": \"invalid\"}";

        kafkaTemplate.send("expense.created", "poison-key", poisonPayload).get(10, TimeUnit.SECONDS);

        // Set up test consumer for DLT topic
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-test-verifier-group");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> dltConsumer = new KafkaConsumer<>(consumerProps)) {
            dltConsumer.subscribe(Collections.singletonList("expense.created.DLT"));

            await().atMost(15, TimeUnit.SECONDS).untilAsserted(() -> {
                var records = dltConsumer.poll(Duration.ofMillis(500));
                boolean foundPoisonRecord = false;
                for (ConsumerRecord<String, String> record : records) {
                    if (record.value() != null && record.value().contains("not-a-valid-uuid")) {
                        foundPoisonRecord = true;
                        break;
                    }
                }
                assertTrue(foundPoisonRecord, "Poison message should be delivered to expense.created.DLT");
            });
        }
    }
}
