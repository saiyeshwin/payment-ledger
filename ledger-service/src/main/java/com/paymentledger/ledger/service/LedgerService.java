package com.paymentledger.ledger.service;

import com.paymentledger.ledger.dto.LedgerEntryResponse;
import com.paymentledger.ledger.entity.LedgerEntry;
import com.paymentledger.ledger.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LedgerService {

    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public List<LedgerEntryResponse> getLedgerEntriesByUser(UUID userId) {
        return ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<LedgerEntryResponse> getLedgerEntriesByExpense(UUID expenseId) {
        return ledgerEntryRepository.findByExpenseId(expenseId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private LedgerEntryResponse mapToResponse(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getExpenseId(),
                entry.getUserId(),
                entry.getAmount(),
                entry.getCurrency(),
                entry.getTransactionType(),
                entry.getStatus(),
                entry.getEventId(),
                entry.getCreatedAt()
        );
    }
}
