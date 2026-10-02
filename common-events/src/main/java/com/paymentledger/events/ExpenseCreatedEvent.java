package com.paymentledger.events;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseCreatedEvent(
    UUID eventId,
    UUID expenseId,
    UUID userId,
    BigDecimal amount,
    String currency,
    String category,
    String description,
    String paymentMethod,
    String status,
    OffsetDateTime createdAt
) {}
