package com.littlehorizons.erp.model.report;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;

/**
 * Everything needed to render an invoice PDF, read from the database.
 *
 * @param lineDescription description of the invoice line (concept and, if any, the charge covered)
 */
public record InvoiceData(
        Invoice invoice,
        Payment payment,
        Student student,
        Guardian guardian,
        PaymentConcept concept,
        String gradeName,
        String lineDescription) {
}
