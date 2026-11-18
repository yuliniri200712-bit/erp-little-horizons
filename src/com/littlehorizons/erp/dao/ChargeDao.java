package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Charge;
import com.littlehorizons.erp.model.enums.ChargeStatus;
import com.littlehorizons.erp.model.report.DebtRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Receivables (cargos), paid amounts and the arrears report. */
public interface ChargeDao {

    int insert(Charge c);

    Optional<Charge> findById(int id);

    void lockForUpdate(int chargeId);

    List<Charge> findByStudent(int studentId, boolean pendingOnly);

    boolean existsDuplicate(int studentId, int conceptId, String description);

    boolean updateStatus(int chargeId, ChargeStatus status);

    BigDecimal paidAmount(int chargeId);

    /** Pending charges whose due date is before {@code cutoff} (e.g. today minus one month). */
    List<DebtRow> overdueBefore(LocalDate cutoff);
}
