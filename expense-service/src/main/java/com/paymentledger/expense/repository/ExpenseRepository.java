package com.paymentledger.expense.repository;

import com.paymentledger.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, UUID>, JpaSpecificationExecutor<Expense> {

    @Query("""
        SELECT 
            COUNT(e) AS totalCount,
            SUM(e.amount) AS totalAmount,
            AVG(e.amount) AS averageAmount,
            MAX(e.amount) AS maxAmount,
            MIN(e.amount) AS minAmount
        FROM Expense e
        WHERE e.userId = :userId
    """)
    ExpenseSummaryProjection getSummaryByUserId(@Param("userId") UUID userId);
}
