package com.paymentledger.expense.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentledger.expense.dto.CreateExpenseRequest;
import com.paymentledger.expense.dto.ExpenseResponse;
import com.paymentledger.expense.dto.ExpenseSummaryResponse;
import com.paymentledger.expense.entity.Expense;
import com.paymentledger.expense.entity.ExpenseCategory;
import com.paymentledger.expense.entity.ExpenseStatus;
import com.paymentledger.expense.entity.PaymentMethod;
import com.paymentledger.expense.exception.AccessDeniedException;
import com.paymentledger.expense.exception.ExpenseNotFoundException;
import com.paymentledger.expense.repository.ExpenseRepository;
import com.paymentledger.expense.repository.ExpenseSummaryProjection;
import com.paymentledger.expense.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ExpenseService expenseService;

    private UUID userId;
    private UUID expenseId;
    private Expense expense;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        expenseId = UUID.randomUUID();
        
        expense = new Expense();
        expense.setId(expenseId);
        expense.setUserId(userId);
        expense.setAmount(new BigDecimal("100.00"));
        expense.setCurrency("INR");
        expense.setCategory(ExpenseCategory.FOOD);
        expense.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        expense.setStatus(ExpenseStatus.COMPLETED);
        expense.setCreatedAt(OffsetDateTime.now());
        expense.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    void createExpense_Success() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                new BigDecimal("100.00"), "INR", ExpenseCategory.FOOD, "Lunch", PaymentMethod.CREDIT_CARD);

        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        ExpenseResponse response = expenseService.createExpense(userId, request);

        assertNotNull(response);
        assertEquals(expenseId, response.id());
        verify(expenseRepository).save(any(Expense.class));
        verify(outboxEventRepository).save(any());
    }

    @Test
    void getExpenseById_Success() {
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        ExpenseResponse response = expenseService.getExpenseById(userId, expenseId);

        assertNotNull(response);
        assertEquals(expenseId, response.id());
    }

    @Test
    void getExpenseById_NotFound() {
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.empty());

        assertThrows(ExpenseNotFoundException.class, () -> expenseService.getExpenseById(userId, expenseId));
    }

    @Test
    void getExpenseById_AccessDenied() {
        UUID otherUserId = UUID.randomUUID();
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        assertThrows(AccessDeniedException.class, () -> expenseService.getExpenseById(otherUserId, expenseId));
    }

    @Test
    void deleteExpense_Success() throws Exception {
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        expenseService.deleteExpense(userId, expenseId);

        verify(expenseRepository).delete(expense);
        verify(outboxEventRepository).save(any());
    }
    
    @Test
    void deleteExpense_AccessDenied() {
        UUID otherUserId = UUID.randomUUID();
        when(expenseRepository.findById(expenseId)).thenReturn(Optional.of(expense));

        assertThrows(AccessDeniedException.class, () -> expenseService.deleteExpense(otherUserId, expenseId));
    }

    @Test
    void getExpenseSummary_Success() {
        ExpenseSummaryProjection projection = mock(ExpenseSummaryProjection.class);
        when(projection.getTotalCount()).thenReturn(5L);
        when(projection.getTotalAmount()).thenReturn(new BigDecimal("500.00"));
        
        when(expenseRepository.getSummaryByUserId(userId)).thenReturn(projection);

        ExpenseSummaryResponse response = expenseService.getExpenseSummary(userId);

        assertNotNull(response);
        assertEquals(5L, response.totalCount());
        assertEquals(new BigDecimal("500.00"), response.totalAmount());
    }
}
