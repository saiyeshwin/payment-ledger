package com.paymentledger.expense.repository;

import java.math.BigDecimal;

public interface ExpenseSummaryProjection {
    Long getTotalCount();
    BigDecimal getTotalAmount();
    BigDecimal getAverageAmount();
    BigDecimal getMaxAmount();
    BigDecimal getMinAmount();
}
