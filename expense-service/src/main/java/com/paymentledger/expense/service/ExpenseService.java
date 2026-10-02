package com.paymentledger.expense.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentledger.expense.dto.CreateExpenseRequest;
import com.paymentledger.expense.dto.ExpenseFilterCriteria;
import com.paymentledger.expense.dto.ExpenseResponse;
import com.paymentledger.expense.dto.ExpenseSummaryResponse;
import com.paymentledger.expense.dto.PagedResponse;
import com.paymentledger.expense.dto.UpdateExpenseRequest;
import com.paymentledger.expense.entity.Expense;
import com.paymentledger.expense.entity.OutboxEvent;
import com.paymentledger.expense.exception.AccessDeniedException;
import com.paymentledger.expense.exception.ExpenseNotFoundException;
import com.paymentledger.expense.repository.ExpenseRepository;
import com.paymentledger.expense.repository.ExpenseSpecifications;
import com.paymentledger.expense.repository.ExpenseSummaryProjection;
import com.paymentledger.expense.repository.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    
    @Value("${kafka.topics.expense-created:expense.created}")
    private String expenseCreatedTopic;

    @Value("${kafka.topics.expense-updated:expense.updated}")
    private String expenseUpdatedTopic;

    @Value("${kafka.topics.expense-deleted:expense.deleted}")
    private String expenseDeletedTopic;

    public ExpenseService(ExpenseRepository expenseRepository, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.expenseRepository = expenseRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    public ExpenseResponse createExpense(UUID userId, CreateExpenseRequest request) {
        Expense expense = new Expense();
        expense.setUserId(userId);
        expense.setAmount(request.amount());
        expense.setCurrency(request.currency());
        expense.setCategory(request.category());
        expense.setDescription(request.description());
        expense.setPaymentMethod(request.paymentMethod());
        
        expense = expenseRepository.save(expense);
        
        createOutboxEvent(expense, "EXPENSE_CREATED");

        return mapToResponse(expense);
    }

    public ExpenseResponse getExpenseById(UUID userId, UUID expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ExpenseNotFoundException("Expense not found"));
                
        if (!expense.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to view this expense");
        }
        
        return mapToResponse(expense);
    }

    public PagedResponse<ExpenseResponse> getExpenses(UUID userId, ExpenseFilterCriteria criteria, Pageable pageable) {
        Page<Expense> page = expenseRepository.findAll(ExpenseSpecifications.buildSpecification(userId, criteria), pageable);
        
        return new PagedResponse<>(
                page.getContent().stream().map(this::mapToResponse).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    public ExpenseResponse updateExpense(UUID userId, UUID expenseId, UpdateExpenseRequest request) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ExpenseNotFoundException("Expense not found"));
                
        if (!expense.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to modify this expense");
        }
        
        if (request.amount() != null) {
            expense.setAmount(request.amount());
        }
        if (request.currency() != null) {
            expense.setCurrency(request.currency());
        }
        if (request.category() != null) {
            expense.setCategory(request.category());
        }
        if (request.description() != null) {
            expense.setDescription(request.description());
        }
        if (request.paymentMethod() != null) {
            expense.setPaymentMethod(request.paymentMethod());
        }
        if (request.status() != null) {
            expense.setStatus(request.status());
        }
        
        expense = expenseRepository.save(expense);
        
        createOutboxEvent(expense, "EXPENSE_UPDATED");
        
        return mapToResponse(expense);
    }

    public void deleteExpense(UUID userId, UUID expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ExpenseNotFoundException("Expense not found"));
                
        if (!expense.getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to delete this expense");
        }
        
        expenseRepository.delete(expense);
        
        createOutboxEvent(expense, "EXPENSE_DELETED");
    }

    public ExpenseSummaryResponse getExpenseSummary(UUID userId) {
        ExpenseSummaryProjection projection = expenseRepository.getSummaryByUserId(userId);
        
        return new ExpenseSummaryResponse(
                userId,
                projection.getTotalCount() != null ? projection.getTotalCount() : 0L,
                projection.getTotalAmount() != null ? projection.getTotalAmount() : BigDecimal.ZERO,
                projection.getAverageAmount() != null ? projection.getAverageAmount() : BigDecimal.ZERO,
                projection.getMaxAmount() != null ? projection.getMaxAmount() : BigDecimal.ZERO,
                projection.getMinAmount() != null ? projection.getMinAmount() : BigDecimal.ZERO,
                "INR" 
        );
    }

    private void createOutboxEvent(Expense expense, String eventType) {
        try {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("id", expense.getId());
            payloadMap.put("userId", expense.getUserId());
            payloadMap.put("amount", expense.getAmount());
            payloadMap.put("currency", expense.getCurrency());
            payloadMap.put("category", expense.getCategory());
            payloadMap.put("status", expense.getStatus());
            
            String payload = objectMapper.writeValueAsString(payloadMap);
            
            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateType("EXPENSE");
            outboxEvent.setAggregateId(expense.getUserId());
            outboxEvent.setEventType(eventType);
            outboxEvent.setPayload(payload);
            
            outboxEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }
    
    private ExpenseResponse mapToResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getUserId(),
                expense.getAmount(),
                expense.getCurrency(),
                expense.getCategory() != null ? expense.getCategory().name() : null,
                expense.getDescription(),
                expense.getPaymentMethod() != null ? expense.getPaymentMethod().name() : null,
                expense.getStatus() != null ? expense.getStatus().name() : null,
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }
}
