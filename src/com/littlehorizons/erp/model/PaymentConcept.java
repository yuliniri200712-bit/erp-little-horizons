package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/** Billing concept (tuition, monthly fee, uniform...). Table: conceptos_pago. */
public record PaymentConcept(
        Integer id,
        String code,
        String name,
        String description,
        BigDecimal baseAmount,
        BigDecimal taxRate,
        boolean recurring,
        boolean active) {
}
