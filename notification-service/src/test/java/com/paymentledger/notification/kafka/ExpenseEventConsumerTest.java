package com.paymentledger.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ExpenseEventConsumerTest {

    private ExpenseEventConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new ExpenseEventConsumer(objectMapper);
    }

    @Test
    void onExpenseCreated_logsNotification() {
        String payload = "{\"userId\":\"u1\",\"expenseId\":\"e1\",\"amount\":100.50,\"currency\":\"USD\",\"category\":\"FOOD\"}";
        assertDoesNotThrow(() -> consumer.onExpenseCreated(payload));
    }

    @Test
    void onExpenseCreated_withInvalidJson_doesNotThrow() {
        String payload = "{invalid json";
        assertDoesNotThrow(() -> consumer.onExpenseCreated(payload));
    }

    @Test
    void onExpenseUpdated_logsNotification() {
        String payload = "{\"userId\":\"u1\",\"expenseId\":\"e1\",\"amount\":150.00,\"currency\":\"USD\",\"status\":\"UPDATED\"}";
        assertDoesNotThrow(() -> consumer.onExpenseUpdated(payload));
    }

    @Test
    void onExpenseDeleted_logsNotification() {
        String payload = "{\"userId\":\"u1\",\"expenseId\":\"e1\"}";
        assertDoesNotThrow(() -> consumer.onExpenseDeleted(payload));
    }
}
