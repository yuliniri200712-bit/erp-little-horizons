package com.littlehorizons.erp.service.invoice;

import com.littlehorizons.erp.model.report.InvoiceData;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Abstraction used by FinanceService to render invoices (Dependency Inversion: the service does
 * not depend on the PDF library; another format could be plugged in without touching it).
 */
public interface InvoiceGenerator {

    /** Renders the invoice and returns the path of the generated file. */
    Path generate(InvoiceData data) throws IOException;
}
