# Concurrency & Idempotency Benchmark Report

## 1. Executive Summary

This report documents the empirical benchmark conducted on the **Payment Ledger Platform** to verify financial consistency, event deduplication efficacy, and throughput under concurrent multi-threaded load.

- **Deduplication Efficacy**: 100% under 100 simultaneous racing duplicate events (0 duplicate ledger entries).
- **Financial Consistency**: 100% exact balance match ($10,000.00 across 100 concurrent financial events).
- **Effective Throughput**: 492.61 ops/sec on 100 parallel workers.
- **Latency Distribution**: P50: 12 ms, P95: 110 ms, P99: 195 ms.

---

## 2. Test Architecture & Methodology

The benchmark was executed using the JUnit 5 test suite [`ConcurrentLedgerIdempotencyBenchmarkTest`](file:///c:/Users/Sai%20Yeshwin/Downloads/D%20Resume/payment-ledger/ledger-service/src/test/java/com/paymentledger/ledger/benchmark/ConcurrentLedgerIdempotencyBenchmarkTest.java) on the `ledger-service`.

### Experiment 1: 100 Concurrent Duplicate Event Race Condition
- **Objective**: Simulate an extreme broker replay / network storm where 100 parallel worker threads attempt to consume and process the *exact same* event payload at the exact same millisecond.
- **Mechanism**: All 100 threads are primed on a `CountDownLatch` and released simultaneously.
- **Expected Outcome**: Exactly 1 ledger entry written; 99 attempts safely filtered via the `processed_events` unique primary key constraint `(event_id, consumer_name)`.

### Experiment 2: 100 Concurrent Financial Transactions
- **Objective**: Measure concurrent financial ingestion throughput, latency percentiles, and balance summation correctness.
- **Mechanism**: 100 distinct expenses ($100.00 each) dispatched across 100 concurrent worker threads targeting the same user ledger.
- **Expected Outcome**: Exactly 100 ledger entries recorded, sum equal to precisely $10,000.00 with zero floating-point loss or race conditions.

---

## 3. Measured Empirical Results

### Experiment 1 Results (Duplicate Race Condition)
```text
=================================================================
BENCHMARK RESULT: 100 Concurrent Duplicate Event Race Condition
-----------------------------------------------------------------
Total Racing Threads    : 100 simultaneous workers
Execution Time          : 172 ms
Total Ledger Entries    : 1 (Expected: 1)
Deduplication Efficacy  : 100% (0 duplicate entries inserted)
Data Integrity          : 100% PASSED
=================================================================
```

### Experiment 2 Results (100 Financial Operations)
```text
=================================================================
BENCHMARK RESULT: 100 Concurrent Financial Transactions
-----------------------------------------------------------------
Total Transactions       : 100
Concurrent Pool Size     : 100 parallel workers
Wall-Clock Duration      : 203 ms
Effective Throughput     : 492.61 ops/sec
Latency P50              : 12 ms
Latency P95              : 110 ms
Latency P99              : 195 ms
Expected Balance Total   : $10,000.00
Actual Recorded Total    : $10,000.00
Financial Consistency    : 100% PERFECT (Exact balance match)
=================================================================
```

---

## 4. Failure Scenario Test Coverage (Testcontainers)

In addition to the in-memory load benchmark, the platform includes Testcontainers-backed integration tests covering distributed failure modes:

| Test Class | Failure Mode Verified | Assertions |
|---|---|---|
| [`LedgerEventConsumerIntegrationTest`](file:///c:/Users/Sai%20Yeshwin/Downloads/D%20Resume/payment-ledger/ledger-service/src/test/java/com/paymentledger/ledger/integration/LedgerEventConsumerIntegrationTest.java) | Duplicate Kafka delivery | Asserts 2 Kafka messages produce 1 DB entry & 1 `processed_events` record. |
| [`LedgerEventConsumerIntegrationTest`](file:///c:/Users/Sai%20Yeshwin/Downloads/D%20Resume/payment-ledger/ledger-service/src/test/java/com/paymentledger/ledger/integration/LedgerEventConsumerIntegrationTest.java) | Poison pill payload | Asserts exponential retry exhaustion and automatic routing to `expense.created.DLT`. |
| [`OutboxPublisherIntegrationTest`](file:///c:/Users/Sai%20Yeshwin/Downloads/D%20Resume/payment-ledger/expense-service/src/test/java/com/paymentledger/expense/integration/OutboxPublisherIntegrationTest.java) | Outbox relay resiliency | Asserts unpublished DB events remain queued until published to Kafka, then marked `published=true`. |
| [`ExpenseServiceIntegrationTest`](file:///c:/Users/Sai%20Yeshwin/Downloads/D%20Resume/payment-ledger/expense-service/src/test/java/com/paymentledger/expense/integration/ExpenseServiceIntegrationTest.java) | Atomicity of outbox write | Asserts expense insertion and outbox event insertion share the exact same local ACID transaction. |
