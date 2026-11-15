package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.BorrowerType;
import com.littlehorizons.erp.model.enums.LoanStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Loan of a resource to a student or a teacher. Tables: prestamos_estudiante / prestamos_profesor. */
public record Loan(
        Integer id,
        BorrowerType borrowerType,
        int resourceId,
        String resourceCode,
        String resourceName,
        int borrowerId,
        String borrowerName,
        LocalDate loanDate,
        LocalDate expectedReturnDate,
        LocalDate actualReturnDate,
        LoanStatus status,
        String notes) {

    /** Days past the expected return date (0 if returned or not overdue). */
    public long daysOverdue() {
        if (actualReturnDate != null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(expectedReturnDate, LocalDate.now()));
    }
}
