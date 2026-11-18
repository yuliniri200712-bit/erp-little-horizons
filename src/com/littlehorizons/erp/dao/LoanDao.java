package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.enums.BorrowerType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Loans to students and teachers. The SQL is built from {@link BorrowerType}, so the same code
 * works on prestamos_estudiante and prestamos_profesor (Open/Closed: a new borrower type only
 * needs a new enum constant). Table and column names come from the enum, never from user input.
 */
public interface LoanDao {

    int insert(Loan loan);

    Optional<Loan> findById(BorrowerType type, int id);

    List<Loan> findOpen(BorrowerType type);

    List<Loan> findOverdue(BorrowerType type);

    List<Loan> findByBorrower(BorrowerType type, int borrowerId);

    List<Loan> findByResource(BorrowerType type, int resourceId);

    boolean markReturned(BorrowerType type, int loanId, LocalDate returnDate, String notes);

    /** Marks as 'Vencido' every active loan whose expected return date has passed. */
    int refreshOverdue(BorrowerType type);

    long countOpenLoans(int resourceId);
}
