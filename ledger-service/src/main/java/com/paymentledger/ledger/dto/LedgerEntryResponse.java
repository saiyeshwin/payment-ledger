package com.paymentledger.ledger.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LedgerEntryResponse(
    UUID id,
    UUID expenseId,
    UUID userId,
    BigDecimal amount,
    String currency,
    String transactionType,
    String status,
    UUID eventId,
    OffsetDateTime createdAt
) {}
