package com.paymentledger.expense.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ExpenseSummaryResponse(
    UUID userId,
    long totalCount,
    BigDecimal totalAmount,
    BigDecimal averageAmount,
    BigDecimal maxAmount,
    BigDecimal minAmount,
    String currency
) {}
