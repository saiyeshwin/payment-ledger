package com.paymentledger.reporting.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CategorySummaryResponse(
    String category,
    BigDecimal totalAmount,
    int transactionCount,
    OffsetDateTime lastUpdated
) {}
