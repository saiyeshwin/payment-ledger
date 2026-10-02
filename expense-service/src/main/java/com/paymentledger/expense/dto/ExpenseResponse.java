package com.paymentledger.expense.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseResponse(
    UUID id,
    UUID userId,
    BigDecimal amount,
    String currency,
    String category,
    String description,
    String paymentMethod,
    String status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
