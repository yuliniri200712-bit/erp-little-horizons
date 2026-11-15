package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.PaymentMethod;
import com.littlehorizons.erp.model.enums.PaymentPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Payment received. Table: pagos. */
public record Payment(
        Integer id,
        int studentId,
        String studentName,
        Integer guardianId,
        String guardianName,
        int conceptId,
        String conceptName,
        Integer chargeId,
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentMethod method,
        String receiptNumber,
        Integer academicYear,
        PaymentPeriod period,
        String notes) {
}
