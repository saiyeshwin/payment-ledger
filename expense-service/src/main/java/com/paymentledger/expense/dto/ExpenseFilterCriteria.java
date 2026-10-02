package com.paymentledger.expense.dto;

import com.paymentledger.expense.entity.ExpenseCategory;
import com.paymentledger.expense.entity.ExpenseStatus;
import com.paymentledger.expense.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExpenseFilterCriteria(
    ExpenseCategory category,
    PaymentMethod paymentMethod,
    ExpenseStatus status,
    OffsetDateTime startDate,
    OffsetDateTime endDate,
    BigDecimal minAmount,
    BigDecimal maxAmount
) {}
