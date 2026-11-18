package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoiceLine;

import java.util.List;
import java.util.Optional;

/** Issued invoices (facturas) and their lines (detalle_factura). */
public interface InvoiceDao {

    int insert(Invoice invoice);

    int insertLine(InvoiceLine line);

    Optional<Invoice> findByNumber(String number);

    /** The invoice that includes the payment, if it was already invoiced. */
    Optional<Invoice> findByPayment(int paymentId);

    List<Invoice> findByStudent(int studentId);

    List<Invoice> findAll();

    List<InvoiceLine> linesOf(int invoiceId);

    boolean updateFile(int invoiceId, String fileName, String path);
}
