package com.paymentledger.reporting.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentledger.reporting.entity.ProcessedEvent;
import com.paymentledger.reporting.entity.ProcessedEventId;
import com.paymentledger.reporting.entity.UserCategoryReport;
import com.paymentledger.reporting.repository.ProcessedEventRepository;
import com.paymentledger.reporting.repository.UserCategoryReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
public class ExpenseEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ExpenseEventConsumer.class);
    private static final String CONSUMER_NAME = "reporting-service";

    private final UserCategoryReportRepository userCategoryReportRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;

    public ExpenseEventConsumer(UserCategoryReportRepository userCategoryReportRepository,
                                ProcessedEventRepository processedEventRepository,
                                ObjectMapper objectMapper) {
        this.userCategoryReportRepository = userCategoryReportRepository;
        this.processedEventRepository = processedEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    @KafkaListener(topics = "${kafka.topics.expense-created}", groupId = "reporting-group")
    public void consumeExpenseCreated(@Payload String payload) {
        try {
            JsonNode eventNode = objectMapper.readTree(payload);
            UUID expenseId = UUID.fromString(eventNode.get("expenseId").asText());
            UUID userId = UUID.fromString(eventNode.get("userId").asText());
            BigDecimal amount = new BigDecimal(eventNode.get("amount").asText());
            String category = eventNode.get("category").asText();

            ProcessedEventId eventId = new ProcessedEventId(expenseId, CONSUMER_NAME);
            if (processedEventRepository.existsById(eventId)) {
                log.warn("Expense event {} already processed by {}, skipping.", expenseId, CONSUMER_NAME);
                return;
            }

            Optional<UserCategoryReport> existingReport = userCategoryReportRepository.findByUserIdAndCategory(userId, category);
            if (existingReport.isPresent()) {
                UserCategoryReport report = existingReport.get();
                report.setTotalAmount(report.getTotalAmount().add(amount));
                report.setTransactionCount(report.getTransactionCount() + 1);
                userCategoryReportRepository.save(report);
            } else {
                UserCategoryReport newReport = new UserCategoryReport();
                newReport.setUserId(userId);
                newReport.setCategory(category);
                newReport.setTotalAmount(amount);
                newReport.setTransactionCount(1);
                userCategoryReportRepository.save(newReport);
            }

            processedEventRepository.save(new ProcessedEvent(eventId));
            log.info("Successfully processed expense event {} for user {} in category {}", expenseId, userId, category);

        } catch (JsonProcessingException e) {
            log.error("Failed to parse expense event payload", e);
        }
    }
}
