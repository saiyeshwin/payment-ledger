package com.paymentledger.reporting.repository;

import com.paymentledger.reporting.entity.UserCategoryReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserCategoryReportRepository extends JpaRepository<UserCategoryReport, UUID> {
    List<UserCategoryReport> findByUserIdOrderByTotalAmountDesc(UUID userId);
    Optional<UserCategoryReport> findByUserIdAndCategory(UUID userId, String category);
}
