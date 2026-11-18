package com.littlehorizons.erp.model.report;

import com.littlehorizons.erp.config.BillingProfile;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoiceLine;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.Student;

import java.util.List;

/**
 * Everything needed to render an invoice (text, TXT file or PDF), read from the database. Rendering
 * only uses saved data, so the files can be regenerated at any time with the same content.
 *
 * @param client   guardian the invoice is issued to (adquiriente)
 * @param payments payments of the lines, in the same order (used for the payment methods)
 */
public record InvoiceData(
        Invoice invoice,
        List<InvoiceLine> lines,
        List<Payment> payments,
        Student student,
        Guardian client,
        String gradeName,
        BillingProfile profile) {
}
