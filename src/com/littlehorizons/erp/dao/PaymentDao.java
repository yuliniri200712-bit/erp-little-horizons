package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.enums.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Payments (pagos) and payment histories. */
public interface PaymentDao {

    int insert(Payment p);

    Optional<Payment> findById(int id);

    Optional<Payment> findByReceipt(String receipt);

    List<Payment> findByStudent(int studentId);

    /** Payments of every student linked to the guardian (covers several children). */
    List<Payment> findByGuardian(int guardianId);

    List<Payment> findRecent(int limit);

    /** Payments of the student that are not in any invoice yet, oldest first. */
    List<Payment> findUnbilledByStudent(int studentId);

    boolean updateDetails(int paymentId, PaymentMethod method, String notes);

    boolean delete(int id);

    BigDecimal totalBetween(LocalDate from, LocalDate to);
}
