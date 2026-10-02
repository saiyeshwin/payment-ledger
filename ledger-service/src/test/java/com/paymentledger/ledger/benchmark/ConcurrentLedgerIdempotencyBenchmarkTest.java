package com.paymentledger.ledger.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentledger.ledger.entity.LedgerEntry;
import com.paymentledger.ledger.kafka.ExpenseEventConsumer;
import com.paymentledger.ledger.repository.LedgerEntryRepository;
import com.paymentledger.ledger.repository.ProcessedEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Tag("benchmark")
public class ConcurrentLedgerIdempotencyBenchmarkTest {

    @Autowired
    private ExpenseEventConsumer expenseEventConsumer;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        ledgerEntryRepository.deleteAll();
        processedEventRepository.deleteAll();
    }

    @Test
    void benchmarkConcurrentDuplicateEventHammer() throws Exception {
        int concurrency = 100;
        UUID duplicateExpenseId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");

        Map<String, Object> payloadMap = new HashMap<>();
        payloadMap.put("expenseId", duplicateExpenseId.toString());
        payloadMap.put("userId", userId.toString());
        payloadMap.put("amount", amount.toString());
        payloadMap.put("currency", "USD");
        payloadMap.put("category", "FOOD");
        payloadMap.put("status", "COMPLETED");

        String payload = objectMapper.writeValueAsString(payloadMap);

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch readyLatch = new CountDownLatch(concurrency);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(concurrency);

        AtomicInteger successfulCreations = new AtomicInteger(0);
        AtomicInteger duplicatesHandled = new AtomicInteger(0);

        for (int i = 0; i < concurrency; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // All 100 threads fire simultaneously
                    expenseEventConsumer.consumeExpenseCreated(payload, "expense.created");
                    successfulCreations.incrementAndGet();
                } catch (Exception e) {
                    duplicatesHandled.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        long wallClockStart = System.currentTimeMillis();
        startLatch.countDown(); // FIRE
        boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
        long wallClockEnd = System.currentTimeMillis();

        executor.shutdown();

        assertTrue(completed, "All 100 concurrent duplicate tasks should complete within timeout");
        long totalTimeMs = wallClockEnd - wallClockStart;

        List<LedgerEntry> entries = ledgerEntryRepository.findByExpenseId(duplicateExpenseId);

        // Verification: Exactly 1 ledger entry exists despite 100 simultaneous racing threads
        assertEquals(1, entries.size(), "Extreme concurrent hammer of 100 identical events MUST yield exactly 1 ledger entry");
        assertEquals(0, amount.compareTo(entries.get(0).getAmount()), "Recorded amount must match");

        System.out.println("=================================================================");
        System.out.println("BENCHMARK RESULT: 100 Concurrent Duplicate Event Race Condition");
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Total Racing Threads    : " + concurrency);
        System.out.println("Execution Time          : " + totalTimeMs + " ms");
        System.out.println("Total Ledger Entries    : " + entries.size() + " (Expected: 1)");
        System.out.println("Deduplication Efficacy  : 100% (0 duplicate entries inserted)");
        System.out.println("Data Integrity          : PASSED");
        System.out.println("=================================================================");
    }

    @Test
    void benchmark100ConcurrentFinancialTransactions() throws Exception {
        int transactionCount = 100;
        BigDecimal unitAmount = new BigDecimal("100.00");
        BigDecimal expectedGrandTotal = unitAmount.multiply(BigDecimal.valueOf(transactionCount)); // $10,000.00
        UUID testUserId = UUID.randomUUID();

        List<String> payloads = new ArrayList<>(transactionCount);
        for (int i = 0; i < transactionCount; i++) {
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("expenseId", UUID.randomUUID().toString());
            payloadMap.put("userId", testUserId.toString());
            payloadMap.put("amount", unitAmount.toString());
            payloadMap.put("currency", "USD");
            payloadMap.put("category", "UTILITIES");
            payloadMap.put("status", "COMPLETED");
            payloads.add(objectMapper.writeValueAsString(payloadMap));
        }

        ExecutorService executor = Executors.newFixedThreadPool(transactionCount);
        CountDownLatch readyLatch = new CountDownLatch(transactionCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(transactionCount);

        List<Long> latenciesMs = new CopyOnWriteArrayList<>();

        for (int i = 0; i < transactionCount; i++) {
            final String payload = payloads.get(i);
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    long start = System.nanoTime();

                    expenseEventConsumer.consumeExpenseCreated(payload, "expense.created");

                    long end = System.nanoTime();
                    latenciesMs.add((end - start) / 1_000_000);
                } catch (Exception e) {
                    System.err.println("Transaction error: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        long wallClockStart = System.currentTimeMillis();
        startLatch.countDown();
        boolean finished = doneLatch.await(15, TimeUnit.SECONDS);
        long wallClockEnd = System.currentTimeMillis();

        executor.shutdown();

        assertTrue(finished, "All 100 financial transactions should complete within timeout");
        long totalDurationMs = wallClockEnd - wallClockStart;
        double throughput = (transactionCount * 1000.0) / (totalDurationMs > 0 ? totalDurationMs : 1);

        List<Long> sortedLatencies = new ArrayList<>(latenciesMs);
        Collections.sort(sortedLatencies);

        long p50 = sortedLatencies.isEmpty() ? 0 : sortedLatencies.get((int) (sortedLatencies.size() * 0.50));
        long p95 = sortedLatencies.isEmpty() ? 0 : sortedLatencies.get((int) (sortedLatencies.size() * 0.95));
        long p99 = sortedLatencies.isEmpty() ? 0 : sortedLatencies.get((int) (sortedLatencies.size() * 0.99));

        List<LedgerEntry> userEntries = ledgerEntryRepository.findByUserIdOrderByCreatedAtDesc(testUserId);
        BigDecimal actualSum = userEntries.stream()
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Verification of 100% financial consistency
        assertEquals(transactionCount, userEntries.size(), "All 100 transactions must be recorded");
        assertEquals(0, expectedGrandTotal.compareTo(actualSum), "Grand total must be exactly $10,000.00 with zero balance loss");

        System.out.println("=================================================================");
        System.out.println("BENCHMARK RESULT: 100 Concurrent Financial Transactions");
        System.out.println("-----------------------------------------------------------------");
        System.out.println("Total Transactions       : " + transactionCount);
        System.out.println("Concurrent Pool Size     : " + transactionCount + " parallel workers");
        System.out.println("Wall-Clock Duration      : " + totalDurationMs + " ms");
        System.out.println("Effective Throughput     : " + String.format("%.2f", throughput) + " ops/sec");
        System.out.println("Latency P50              : " + p50 + " ms");
        System.out.println("Latency P95              : " + p95 + " ms");
        System.out.println("Latency P99              : " + p99 + " ms");
        System.out.println("Expected Balance Total   : $" + expectedGrandTotal);
        System.out.println("Actual Recorded Total    : $" + actualSum);
        System.out.println("Financial Consistency    : 100% PERFECT (Exact balance match)");
        System.out.println("=================================================================");
    }
}
