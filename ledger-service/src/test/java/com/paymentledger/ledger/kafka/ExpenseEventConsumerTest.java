package com.paymentledger.ledger.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentledger.ledger.entity.LedgerEntry;
import com.paymentledger.ledger.entity.ProcessedEvent;
import com.paymentledger.ledger.entity.ProcessedEventId;
import com.paymentledger.ledger.repository.LedgerEntryRepository;
import com.paymentledger.ledger.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseEventConsumerTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    private ExpenseEventConsumer expenseEventConsumer;

    @BeforeEach
    void setUp() {
        // Mocks are already initialised by MockitoExtension before @BeforeEach runs
        expenseEventConsumer = new ExpenseEventConsumer(
                ledgerEntryRepository,
                processedEventRepository,
                new ObjectMapper()
        );
    }

    @Test
    void consumeExpenseCreated_whenFirstTime_savesLedgerEntryAndProcessedEvent() throws Exception {
        UUID expenseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String payload = String.format(
                "{\"expenseId\":\"%s\",\"userId\":\"%s\",\"amount\":\"100.00\",\"currency\":\"USD\"}",
                expenseId, userId);

        when(processedEventRepository.existsById(any(ProcessedEventId.class))).thenReturn(false);
        when(ledgerEntryRepository.save(any(LedgerEntry.class))).thenAnswer(invocation -> {
            LedgerEntry entry = invocation.getArgument(0);
            entry.setId(UUID.randomUUID());
            return entry;
        });

        expenseEventConsumer.consumeExpenseCreated(payload, "expense.created");

        verify(ledgerEntryRepository).save(any(LedgerEntry.class));
        verify(processedEventRepository).save(any(ProcessedEvent.class));
    }

    @Test
    void consumeExpenseCreated_whenDuplicate_skipsProcessing() {
        UUID expenseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String payload = String.format(
                "{\"expenseId\":\"%s\",\"userId\":\"%s\",\"amount\":\"100.00\",\"currency\":\"USD\"}",
                expenseId, userId);

        when(processedEventRepository.existsById(any(ProcessedEventId.class))).thenReturn(true);

        expenseEventConsumer.consumeExpenseCreated(payload, "expense.created");

        verify(ledgerEntryRepository, never()).save(any(LedgerEntry.class));
        verify(processedEventRepository, never()).save(any(ProcessedEvent.class));
    }
}
