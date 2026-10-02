package com.paymentledger.expense.dto;

import com.paymentledger.expense.entity.ExpenseCategory;
import com.paymentledger.expense.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateExpenseRequest(
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    BigDecimal amount,
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter code")
    String currency,
    
    @NotNull(message = "Category is required")
    ExpenseCategory category,
    
    String description,
    
    @NotNull(message = "Payment method is required")
    PaymentMethod paymentMethod
) {}
