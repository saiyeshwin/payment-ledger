package com.paymentledger.events;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ExpenseDeletedEvent(
    UUID eventId,
    UUID expenseId,
    UUID userId,
    OffsetDateTime deletedAt
) {}
