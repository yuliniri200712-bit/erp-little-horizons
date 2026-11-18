package com.littlehorizons.erp.model.dto;

import com.littlehorizons.erp.model.Payment;

/**
 * Outcome of a confirmed payment: the stored payment and the electronic invoice issued for it.
 * When the invoice could not be issued (for example, the resolution expired) {@code invoice} is
 * null and {@code invoiceError} says why: the payment is kept and can be invoiced later.
 */
public record PaymentResult(Payment payment, InvoiceDocument invoice, String invoiceError) {
}
