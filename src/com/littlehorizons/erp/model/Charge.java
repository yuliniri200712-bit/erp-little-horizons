package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.ChargeStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Receivable (amount owed by a student for a concept). Table: cargos. */
public record Charge(
        Integer id,
        int studentId,
        String studentName,
        int conceptId,
        String conceptName,
        String description,
        BigDecimal amount,
        BigDecimal paidAmount,
        LocalDate issueDate,
        LocalDate dueDate,
        ChargeStatus status) {

    public BigDecimal pending() {
        BigDecimal paid = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        return amount.subtract(paid).max(BigDecimal.ZERO);
    }
}
