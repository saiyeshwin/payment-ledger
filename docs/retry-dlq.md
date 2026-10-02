# Retry Strategy & Dead Letter Queues (DLQ)

## Why Retries Are Needed

In distributed systems, transient failures are common. Examples include:
- A downstream database is momentarily restarting.
- A temporary network partition prevents API access.
- Optimistic locking failures.

We handle these by automatically retrying message processing before considering it a total failure.

## Spring Kafka Configuration

We utilize Spring Kafka's `DefaultErrorHandler` to manage retry policies and error recovery.

### ExponentialBackOff Settings

Instead of rapid-fire retries, we use an `ExponentialBackOff` strategy:
- **Initial Interval**: 1000ms (1 second)
- **Multiplier**: 2.0
- **Max Interval**: 10000ms (10 seconds)
- **Max Attempts**: 3

If a message fails, it will be retried after 1s, then 2s, then 4s.

## DeadLetterPublishingRecoverer and DLT Routing

If a message continues to fail after all retry attempts (e.g., a "poison pill" message with invalid JSON, or a persistent database outage), it is routed to a Dead Letter Topic (DLT) using `DeadLetterPublishingRecoverer`.

- **Naming Convention**: `{original-topic}.DLT`
  - Example: Failed messages from `expense.created` are sent to `expense.created.DLT`.

This prevents a single bad message from blocking the entire partition (head-of-line blocking).

## Inspecting and Replaying DLT Messages

Messages in the DLT contain header metadata indicating the reason for failure (exception stack trace, original topic, partition, offset).

Engineers can inspect these messages using Kafka CLI tools or UI (e.g., AKHQ, Kafdrop). Once the underlying bug is fixed, messages from the `.DLT` topic can be manually or automatically replayed back into the main topic.

## Per-Service Retry Strategy

| Service | Retry Policy | Max Attempts | Fallback Action |
|---|---|---|---|
| Ledger Service | Exponential (1s -> 10s) | 3 | Route to DLT |
| Reporting Service | Exponential (1s -> 10s) | 3 | Route to DLT |
| Notification Service | Fixed Delay (5s) | 2 | Route to DLT (Log & Ignore) |
