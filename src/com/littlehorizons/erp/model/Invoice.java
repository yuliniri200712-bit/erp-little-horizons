package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Invoice issued for a payment (simulated electronic invoice). Table: facturas. */
public record Invoice(
        Integer id,
        String number,
        int paymentId,
        LocalDate issueDate,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        String cufe,
        String fileName,
        String filePath) {
}
