package com.paymentledger.expense.repository;

import com.paymentledger.expense.dto.ExpenseFilterCriteria;
import com.paymentledger.expense.entity.Expense;
import com.paymentledger.expense.entity.ExpenseCategory;
import com.paymentledger.expense.entity.ExpenseStatus;
import com.paymentledger.expense.entity.PaymentMethod;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ExpenseSpecifications {

    public static Specification<Expense> byUserId(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<Expense> byCategory(ExpenseCategory category) {
        return (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
    }

    public static Specification<Expense> byPaymentMethod(PaymentMethod paymentMethod) {
        return (root, query, cb) -> paymentMethod == null ? null : cb.equal(root.get("paymentMethod"), paymentMethod);
    }

    public static Specification<Expense> byStatus(ExpenseStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Expense> afterDate(OffsetDateTime startDate) {
        return (root, query, cb) -> startDate == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
    }

    public static Specification<Expense> beforeDate(OffsetDateTime endDate) {
        return (root, query, cb) -> endDate == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
    }

    public static Specification<Expense> amountGreaterThanOrEqual(BigDecimal minAmount) {
        return (root, query, cb) -> minAmount == null ? null : cb.greaterThanOrEqualTo(root.get("amount"), minAmount);
    }

    public static Specification<Expense> amountLessThanOrEqual(BigDecimal maxAmount) {
        return (root, query, cb) -> maxAmount == null ? null : cb.lessThanOrEqualTo(root.get("amount"), maxAmount);
    }

    public static Specification<Expense> buildSpecification(UUID userId, ExpenseFilterCriteria criteria) {
        Specification<Expense> spec = Specification.where(byUserId(userId));

        if (criteria != null) {
            if (criteria.category() != null) {
                spec = spec.and(byCategory(criteria.category()));
            }
            if (criteria.paymentMethod() != null) {
                spec = spec.and(byPaymentMethod(criteria.paymentMethod()));
            }
            if (criteria.status() != null) {
                spec = spec.and(byStatus(criteria.status()));
            }
            if (criteria.startDate() != null) {
                spec = spec.and(afterDate(criteria.startDate()));
            }
            if (criteria.endDate() != null) {
                spec = spec.and(beforeDate(criteria.endDate()));
            }
            if (criteria.minAmount() != null) {
                spec = spec.and(amountGreaterThanOrEqual(criteria.minAmount()));
            }
            if (criteria.maxAmount() != null) {
                spec = spec.and(amountLessThanOrEqual(criteria.maxAmount()));
            }
        }

        return spec;
    }
}
