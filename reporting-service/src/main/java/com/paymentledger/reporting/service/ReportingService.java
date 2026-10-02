package com.paymentledger.reporting.service;

import com.paymentledger.reporting.dto.CategorySummaryResponse;
import com.paymentledger.reporting.dto.ReportSummaryResponse;
import com.paymentledger.reporting.entity.UserCategoryReport;
import com.paymentledger.reporting.repository.UserCategoryReportRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportingService {

    private final UserCategoryReportRepository userCategoryReportRepository;

    public ReportingService(UserCategoryReportRepository userCategoryReportRepository) {
        this.userCategoryReportRepository = userCategoryReportRepository;
    }

    public ReportSummaryResponse getReportSummary(UUID userId) {
        List<UserCategoryReport> reports = userCategoryReportRepository.findByUserIdOrderByTotalAmountDesc(userId);

        List<CategorySummaryResponse> categories = reports.stream()
                .map(report -> new CategorySummaryResponse(
                        report.getCategory(),
                        report.getTotalAmount(),
                        report.getTransactionCount(),
                        report.getLastUpdated()
                ))
                .collect(Collectors.toList());

        BigDecimal grandTotal = categories.stream()
                .map(CategorySummaryResponse::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalTransactions = categories.stream()
                .mapToInt(CategorySummaryResponse::transactionCount)
                .sum();

        return new ReportSummaryResponse(userId, categories, grandTotal, totalTransactions);
    }
}
