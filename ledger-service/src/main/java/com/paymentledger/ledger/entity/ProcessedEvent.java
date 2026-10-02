package com.paymentledger.ledger.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {

    @EmbeddedId
    private ProcessedEventId id;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    public ProcessedEvent() {
    }

    public ProcessedEvent(ProcessedEventId id) {
        this.id = id;
    }

    public ProcessedEvent(ProcessedEventId id, OffsetDateTime processedAt) {
        this.id = id;
        this.processedAt = processedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.processedAt == null) {
            this.processedAt = OffsetDateTime.now();
        }
    }

    public ProcessedEventId getId() {
        return id;
    }

    public void setId(ProcessedEventId id) {
        this.id = id;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
