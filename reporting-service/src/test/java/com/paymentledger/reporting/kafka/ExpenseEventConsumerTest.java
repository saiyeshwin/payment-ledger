package com.paymentledger.reporting.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentledger.reporting.entity.ProcessedEvent;
import com.paymentledger.reporting.entity.ProcessedEventId;
import com.paymentledger.reporting.entity.UserCategoryReport;
import com.paymentledger.reporting.repository.ProcessedEventRepository;
import com.paymentledger.reporting.repository.UserCategoryReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExpenseEventConsumerTest {

    @Mock
    private UserCategoryReportRepository userCategoryReportRepository;
    @Mock
    private ProcessedEventRepository processedEventRepository;

    private ObjectMapper objectMapper;
    private ExpenseEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new ExpenseEventConsumer(userCategoryReportRepository, processedEventRepository, objectMapper);
    }

    @Test
    void consumeExpenseCreated_whenNewCategory_createsReport() {
        UUID expenseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String payload = String.format("{\"expenseId\":\"%s\",\"userId\":\"%s\",\"amount\":150.00,\"category\":\"Books\"}", expenseId, userId);

        when(processedEventRepository.existsById(any(ProcessedEventId.class))).thenReturn(false);
        when(userCategoryReportRepository.findByUserIdAndCategory(userId, "Books")).thenReturn(Optional.empty());

        consumer.consumeExpenseCreated(payload);

        ArgumentCaptor<UserCategoryReport> reportCaptor = ArgumentCaptor.forClass(UserCategoryReport.class);
        verify(userCategoryReportRepository).save(reportCaptor.capture());

        UserCategoryReport savedReport = reportCaptor.getValue();
        assertEquals(userId, savedReport.getUserId());
        assertEquals("Books", savedReport.getCategory());
        assertEquals(0, new BigDecimal("150.00").compareTo(savedReport.getTotalAmount()), "Total amount should be 150.00");
        assertEquals(1, savedReport.getTransactionCount());

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void consumeExpenseCreated_whenExistingCategory_updatesReport() {
        UUID expenseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String payload = String.format("{\"expenseId\":\"%s\",\"userId\":\"%s\",\"amount\":50.00,\"category\":\"Food\"}", expenseId, userId);

        UserCategoryReport existingReport = new UserCategoryReport(UUID.randomUUID(), userId, "Food", new BigDecimal("100.00"), 2, OffsetDateTime.now());

        when(processedEventRepository.existsById(any(ProcessedEventId.class))).thenReturn(false);
        when(userCategoryReportRepository.findByUserIdAndCategory(userId, "Food")).thenReturn(Optional.of(existingReport));

        consumer.consumeExpenseCreated(payload);

        verify(userCategoryReportRepository).save(existingReport);
        assertEquals(new BigDecimal("150.00"), existingReport.getTotalAmount());
        assertEquals(3, existingReport.getTransactionCount());

        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void consumeExpenseCreated_whenDuplicate_skipsProcessing() {
        UUID expenseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String payload = String.format("{\"expenseId\":\"%s\",\"userId\":\"%s\",\"amount\":50.00,\"category\":\"Food\"}", expenseId, userId);

        when(processedEventRepository.existsById(any(ProcessedEventId.class))).thenReturn(true);

        consumer.consumeExpenseCreated(payload);

        verify(userCategoryReportRepository, never()).findByUserIdAndCategory(any(), any());
        verify(userCategoryReportRepository, never()).save(any());
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }
}
