package com.littlehorizons.erp.model.report;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Overdue receivable with the responsible guardian, used by the arrears report. */
public record DebtRow(
        int chargeId,
        int studentId,
        String studentName,
        String gradeName,
        String guardianName,
        String guardianPhone,
        String conceptName,
        String description,
        BigDecimal amount,
        BigDecimal paid,
        LocalDate dueDate,
        long daysOverdue) {

    public BigDecimal pending() {
        return amount.subtract(paid == null ? BigDecimal.ZERO : paid);
    }
}
