package com.paymentledger.reporting.repository;

import com.paymentledger.reporting.entity.ProcessedEvent;
import com.paymentledger.reporting.entity.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEventId> {
    boolean existsById(ProcessedEventId id);
}
