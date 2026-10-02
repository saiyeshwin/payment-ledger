package com.paymentledger.ledger.service;

import com.paymentledger.ledger.dto.LedgerEntryResponse;
import com.paymentledger.ledger.entity.LedgerEntry;
import com.paymentledger.ledger.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

class LedgerServiceTest {

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getLedgerEntriesByUser() {
        UUID userId = UUID.randomUUID();
        LedgerEntry entry = new LedgerEntry(UUID.randomUUID(), UUID.randomUUID(), userId, new BigDecimal("100.00"), "USD", "DEBIT", "COMPLETED", UUID.randomUUID(), OffsetDateTime.now());
        when(ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(entry));

        List<LedgerEntryResponse> responses = ledgerService.getLedgerEntriesByUser(userId);

        assertEquals(1, responses.size());
        assertEquals(userId, responses.get(0).userId());
    }

    @Test
    void getLedgerEntriesByExpense() {
        UUID expenseId = UUID.randomUUID();
        LedgerEntry entry = new LedgerEntry(UUID.randomUUID(), expenseId, UUID.randomUUID(), new BigDecimal("100.00"), "USD", "DEBIT", "COMPLETED", UUID.randomUUID(), OffsetDateTime.now());
        when(ledgerEntryRepository.findByExpenseId(expenseId)).thenReturn(List.of(entry));

        List<LedgerEntryResponse> responses = ledgerService.getLedgerEntriesByExpense(expenseId);

        assertEquals(1, responses.size());
        assertEquals(expenseId, responses.get(0).expenseId());
    }
}
