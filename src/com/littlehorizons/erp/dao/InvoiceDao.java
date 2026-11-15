package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Invoice;

import java.util.List;
import java.util.Optional;

/** Issued invoices (facturas). */
public class InvoiceDao extends BaseDao {

    private static final RowMapper<Invoice> MAPPER = rs -> new Invoice(
            rs.getInt("id_factura"), rs.getString("numero_factura"), rs.getInt("id_pago"), localDate(rs, "fecha_emision"),
            rs.getBigDecimal("subtotal"), rs.getBigDecimal("valor_iva"), rs.getBigDecimal("total"), rs.getString("cufe"),
            rs.getString("nombre_archivo_pdf"), rs.getString("ruta_archivo"));

    public int insert(Invoice i) {
        return insert("INSERT INTO facturas (numero_factura, id_pago, fecha_emision, subtotal, valor_iva, total, cufe, "
                        + "nombre_archivo_pdf, ruta_archivo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", i.number(), i.paymentId(),
                i.issueDate(), i.subtotal(), i.tax(), i.total(), i.cufe(), i.fileName(), i.filePath());
    }

    public Optional<Invoice> findByPayment(int paymentId) {
        return queryOne("SELECT * FROM facturas WHERE id_pago = ?", MAPPER, paymentId);
    }

    public Optional<Invoice> findByNumber(String number) {
        return queryOne("SELECT * FROM facturas WHERE numero_factura = ?", MAPPER, number);
    }

    public List<Invoice> findAll() {
        return query("SELECT * FROM facturas ORDER BY id_factura DESC", MAPPER);
    }

    public boolean updateFile(int invoiceId, String fileName, String path) {
        return update("UPDATE facturas SET nombre_archivo_pdf = ?, ruta_archivo = ? WHERE id_factura = ?", fileName, path, invoiceId) > 0;
    }
}
