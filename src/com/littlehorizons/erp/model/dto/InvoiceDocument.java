package com.littlehorizons.erp.model.dto;

import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.report.InvoiceData;

import java.nio.file.Path;
import java.util.List;

/**
 * An invoice with its printable text and the files where it is saved (TXT and PDF).
 * {@code fileError} is set when the invoice was issued but its files could not be written: the
 * invoice still exists and the files can be generated again from the invoices menu.
 */
public record InvoiceDocument(InvoiceData data, List<String> text, List<Path> files, String fileError) {

    public Invoice invoice() {
        return data.invoice();
    }
}
