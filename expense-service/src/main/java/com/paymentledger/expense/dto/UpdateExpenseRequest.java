package com.paymentledger.expense.dto;

import com.paymentledger.expense.entity.ExpenseCategory;
import com.paymentledger.expense.entity.ExpenseStatus;
import com.paymentledger.expense.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record UpdateExpenseRequest(
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    BigDecimal amount,
    String currency,
    ExpenseCategory category,
    String description,
    PaymentMethod paymentMethod,
    ExpenseStatus status
) {}
