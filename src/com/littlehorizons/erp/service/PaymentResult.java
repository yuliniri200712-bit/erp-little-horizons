package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;

import java.nio.file.Path;

/**
 * Outcome of a confirmed payment: the stored payment, its invoice and the PDF path.
 * {@code pdfError} is set when the payment was saved but the PDF could not be written.
 */
public record PaymentResult(Payment payment, Invoice invoice, Path pdfPath, String pdfError) {
}
