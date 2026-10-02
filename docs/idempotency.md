# Idempotent Consumers

## Why At-Least-Once Delivery Requires Idempotency

Apache Kafka provides "at-least-once" delivery semantics by default. This means a consumer might receive the same message more than once due to network partitions, consumer rebalances, or producer retries. To ensure system accuracy (e.g., preventing duplicate journal entries in the ledger or skewed aggregates in reports), consumers implement **idempotent event processing using persistent event deduplication to prevent duplicate processing effects**.

## The `processed_events` Table Design

Each consuming service maintains a `processed_events` table in its own database to track which messages have already been handled.

### SQL Schema
```sql
CREATE TABLE processed_events (
    event_id UUID NOT NULL,
    consumer_name VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id, consumer_name)
);
```
The composite primary key `(event_id, consumer_name)` enforces uniqueness.

## Code Flow and Atomic Check-and-Insert

When a message is received:
1. **Begin Transaction**.
2. **Check-and-Insert**: The service attempts to insert a record into `processed_events` with the message's unique `eventId`.
3. **Conflict Handling**:
    - If the insert **succeeds**, this is the first time seeing the message. The business logic executes.
    - If the insert **fails** with a Unique Constraint Violation, the message has been processed before. The transaction rolls back, the business logic is skipped, and the Kafka offset is acknowledged.
4. **Commit Transaction**.

This approach ensures atomic deduplication, preventing race conditions if duplicate messages are processed concurrently.

## Implementation Independence

Because each service has its own database, idempotency is implemented completely locally. The Ledger Service deduplicates messages in `ledger_db`, and the Reporting Service deduplicates in `reporting_db`. There is no centralized idempotency store, removing single points of failure.
