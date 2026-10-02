package com.paymentledger.notification.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class ExpenseEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ExpenseEventConsumer.class);

    private final ObjectMapper objectMapper;

    public ExpenseEventConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${kafka.topics.expense-created}", groupId = "notification-group")
    public void onExpenseCreated(@Payload String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String userId = root.path("userId").asText(null);
            String expenseId = root.path("expenseId").asText(null);
            String amount = root.path("amount").asText(null);
            String currency = root.path("currency").asText(null);
            String category = root.path("category").asText(null);

            log.info("[NOTIFICATION] New expense recorded | userId={} | expenseId={} | amount={} {} | category={}",
                    userId, expenseId, amount, currency, category);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse expense-created payload: {}", payload, e);
        } catch (Exception e) {
            log.error("Error processing expense-created payload: {}", payload, e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.expense-updated}", groupId = "notification-group")
    public void onExpenseUpdated(@Payload String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String userId = root.path("userId").asText(null);
            String expenseId = root.path("expenseId").asText(null);
            String newAmount = root.path("amount").asText(null);
            String currency = root.path("currency").asText(null);
            String status = root.path("status").asText(null);

            log.info("[NOTIFICATION] Expense updated | userId={} | expenseId={} | newAmount={} {} | status={}",
                    userId, expenseId, newAmount, currency, status);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse expense-updated payload: {}", payload, e);
        } catch (Exception e) {
            log.error("Error processing expense-updated payload: {}", payload, e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.expense-deleted}", groupId = "notification-group")
    public void onExpenseDeleted(@Payload String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String userId = root.path("userId").asText(null);
            String expenseId = root.path("expenseId").asText(null);

            log.info("[NOTIFICATION] Expense deleted | userId={} | expenseId={}",
                    userId, expenseId);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse expense-deleted payload: {}", payload, e);
        } catch (Exception e) {
            log.error("Error processing expense-deleted payload: {}", payload, e);
        }
    }
}
