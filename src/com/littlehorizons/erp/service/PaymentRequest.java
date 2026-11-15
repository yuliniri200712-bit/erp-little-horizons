package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.enums.PaymentMethod;
import com.littlehorizons.erp.model.enums.PaymentPeriod;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data captured by the view to register a payment.
 *
 * @param guardianId paying guardian; {@code null} uses the student's financial guardian
 * @param chargeId   receivable covered by the payment; {@code null} for a payment without charge
 */
public record PaymentRequest(
        int studentId,
        Integer guardianId,
        int conceptId,
        Integer chargeId,
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentMethod method,
        PaymentPeriod period,
        String notes) {
}
