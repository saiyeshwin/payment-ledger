package com.paymentledger.ledger.repository;

import com.paymentledger.ledger.entity.ProcessedEvent;
import com.paymentledger.ledger.entity.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEventId> {
    boolean existsById(ProcessedEventId id);
}
