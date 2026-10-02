package com.paymentledger.reporting.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ReportSummaryResponse(
    UUID userId,
    List<CategorySummaryResponse> categories,
    BigDecimal grandTotal,
    int totalTransactions
) {}
