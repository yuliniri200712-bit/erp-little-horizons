package com.littlehorizons.erp.service.invoice;

import com.littlehorizons.erp.model.report.InvoiceData;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Strategy used by FinanceService to save invoices in a file format (Dependency Inversion: the service
 * does not depend on the PDF library). The project plugs in two strategies, TXT and PDF; another format
 * can be added without touching the service (Open/Closed).
 */
public interface InvoiceGenerator {

    /** Short name of the format, e.g. "TXT" or "PDF". */
    String format();

    /** File where the invoice with that number is (or would be) saved: factura_000123.txt / .pdf. */
    Path pathFor(String invoiceNumber);

    /** Writes the file (replacing it if it exists, with the same content) and returns its path. */
    Path generate(InvoiceData data) throws IOException;
}
