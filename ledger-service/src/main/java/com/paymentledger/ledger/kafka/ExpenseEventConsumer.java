package com.paymentledger.ledger.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentledger.ledger.entity.LedgerEntry;
import com.paymentledger.ledger.entity.ProcessedEvent;
import com.paymentledger.ledger.entity.ProcessedEventId;
import com.paymentledger.ledger.repository.LedgerEntryRepository;
import com.paymentledger.ledger.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class ExpenseEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ExpenseEventConsumer.class);

    private final LedgerEntryRepository ledgerEntryRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public ExpenseEventConsumer(LedgerEntryRepository ledgerEntryRepository,
                                ProcessedEventRepository processedEventRepository,
                                ObjectMapper objectMapper) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @KafkaListener(topics = "${kafka.topics.expense-created}", groupId = "ledger-group")
    public void consumeExpenseCreated(@Payload String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            UUID expenseId = UUID.fromString(node.get("expenseId").asText());
            UUID eventId = expenseId; // Using expenseId as eventId for idempotency
            
            ProcessedEventId processedEventId = new ProcessedEventId(eventId, "ledger-service");
            if (processedEventRepository.existsById(processedEventId)) {
                log.warn("Event {} already processed by ledger-service. Skipping.", eventId);
                return;
            }

            UUID userId = UUID.fromString(node.get("userId").asText());
            BigDecimal amount = new BigDecimal(node.get("amount").asText());
            String currency = node.has("currency") ? node.get("currency").asText() : "USD";
            
            LedgerEntry ledgerEntry = new LedgerEntry();
            ledgerEntry.setExpenseId(expenseId);
            ledgerEntry.setUserId(userId);
            ledgerEntry.setAmount(amount);
            ledgerEntry.setCurrency(currency);
            ledgerEntry.setTransactionType("DEBIT");
            ledgerEntry.setStatus("COMPLETED");
            ledgerEntry.setEventId(eventId);
            
            LedgerEntry savedEntry = ledgerEntryRepository.save(ledgerEntry);
            processedEventRepository.save(new ProcessedEvent(processedEventId));
            
            log.info("Processed expense event {} into ledger entry {}", expenseId, savedEntry.getId());
            
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Concurrent duplicate event detected via unique constraint on processed_events. Safely skipped.");
        } catch (Exception e) {
            log.error("Failed to process expense event", e);
            throw new RuntimeException("Error processing message", e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.expense-created-dlt}", groupId = "ledger-group-dlt")
    public void handleDlt(@Payload String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("DLT message received on topic: {}, payload: {}", topic, payload);
    }
}
