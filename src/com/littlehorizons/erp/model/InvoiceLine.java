package com.littlehorizons.erp.model;

import java.math.BigDecimal;

/**
 * One line of an invoice: one payment. The amount paid already includes the VAT of its concept, so
 * the line splits it: unit value without VAT + VAT = line total (exactly what was paid).
 * Table: detalle_factura.
 */
public record InvoiceLine(
        Integer id,
        Integer invoiceId,
        int paymentId,
        String receiptNumber,
        String description,
        int quantity,
        BigDecimal unitValue,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal lineTotal) {

    /** Value without VAT (quantity x unit value). */
    public BigDecimal subtotal() {
        return unitValue.multiply(BigDecimal.valueOf(quantity));
    }

    public InvoiceLine withInvoiceId(int newInvoiceId) {
        return new InvoiceLine(id, newInvoiceId, paymentId, receiptNumber, description, quantity, unitValue, taxRate,
                taxAmount, lineTotal);
    }
}
