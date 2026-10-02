package com.paymentledger.expense.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentledger.expense.dto.CreateExpenseRequest;
import com.paymentledger.expense.dto.ExpenseResponse;
import com.paymentledger.expense.dto.ExpenseSummaryResponse;
import com.paymentledger.expense.entity.ExpenseCategory;
import com.paymentledger.expense.entity.ExpenseStatus;
import com.paymentledger.expense.entity.PaymentMethod;
import com.paymentledger.expense.security.JwtAuthenticationFilter;
import com.paymentledger.expense.security.JwtTokenProvider;
import com.paymentledger.expense.security.UserPrincipal;
import com.paymentledger.expense.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExpenseController.class)
@AutoConfigureMockMvc(addFilters = false)
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExpenseService expenseService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID userId;
    private UUID expenseId;
    private ExpenseResponse expenseResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        expenseId = UUID.randomUUID();
        
        UserPrincipal principal = new UserPrincipal(userId, "test@example.com");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );

        expenseResponse = new ExpenseResponse(
                expenseId, userId, new BigDecimal("100.00"), "INR", "FOOD", "Lunch", "CREDIT_CARD", "COMPLETED", OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    @Test
    void createExpense_Success() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                new BigDecimal("100.00"), "INR", ExpenseCategory.FOOD, "Lunch", PaymentMethod.CREDIT_CARD
        );

        when(expenseService.createExpense(eq(userId), any(CreateExpenseRequest.class))).thenReturn(expenseResponse);

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(expenseId.toString()));
    }

    @Test
    void createExpense_InvalidRequest() throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                new BigDecimal("-10.00"), "INR", ExpenseCategory.FOOD, "Lunch", PaymentMethod.CREDIT_CARD
        );

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getExpense_Success() throws Exception {
        when(expenseService.getExpenseById(userId, expenseId)).thenReturn(expenseResponse);

        mockMvc.perform(get("/api/expenses/{id}", expenseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(expenseId.toString()));
    }

    @Test
    void getSummary_Success() throws Exception {
        ExpenseSummaryResponse summaryResponse = new ExpenseSummaryResponse(
                userId, 1L, new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("100.00"), "INR"
        );
        
        when(expenseService.getExpenseSummary(userId)).thenReturn(summaryResponse);

        mockMvc.perform(get("/api/expenses/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
    }

    @Test
    void deleteExpense_Success() throws Exception {
        mockMvc.perform(delete("/api/expenses/{id}", expenseId))
                .andExpect(status().isNoContent());
    }
}
