package com.paymentledger.ledger.repository;

import com.paymentledger.ledger.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<LedgerEntry> findByExpenseId(UUID expenseId);
}
