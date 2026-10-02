package com.paymentledger.reporting.service;

import com.paymentledger.reporting.dto.ReportSummaryResponse;
import com.paymentledger.reporting.entity.UserCategoryReport;
import com.paymentledger.reporting.repository.UserCategoryReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReportingServiceTest {

    @Mock
    private UserCategoryReportRepository repository;

    private ReportingService service;

    @BeforeEach
    void setUp() {
        service = new ReportingService(repository);
    }

    @Test
    void getReportSummary_returnsAggregatedData() {
        UUID userId = UUID.randomUUID();
        UserCategoryReport r1 = new UserCategoryReport(UUID.randomUUID(), userId, "Food", new BigDecimal("100.00"), 2, OffsetDateTime.now());
        UserCategoryReport r2 = new UserCategoryReport(UUID.randomUUID(), userId, "Transport", new BigDecimal("50.50"), 1, OffsetDateTime.now());

        when(repository.findByUserIdOrderByTotalAmountDesc(userId)).thenReturn(Arrays.asList(r1, r2));

        ReportSummaryResponse response = service.getReportSummary(userId);

        assertEquals(userId, response.userId());
        assertEquals(2, response.categories().size());
        assertEquals(new BigDecimal("150.50"), response.grandTotal());
        assertEquals(3, response.totalTransactions());
    }

    @Test
    void getReportSummary_whenNoData_returnsEmptyReport() {
        UUID userId = UUID.randomUUID();
        when(repository.findByUserIdOrderByTotalAmountDesc(userId)).thenReturn(Collections.emptyList());

        ReportSummaryResponse response = service.getReportSummary(userId);

        assertEquals(userId, response.userId());
        assertEquals(0, response.categories().size());
        assertEquals(BigDecimal.ZERO, response.grandTotal());
        assertEquals(0, response.totalTransactions());
    }
}
