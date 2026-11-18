package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoiceLine;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link InvoiceDao}: issued invoices (facturas) and their lines (detalle_factura). */
public class JdbcInvoiceDao extends BaseDao implements InvoiceDao {

    private static final String SELECT = "SELECT f.*, " + fullNameSql("e") + " AS nombre_estudiante, "
            + fullNameSql("a") + " AS nombre_acudiente, "
            + "(SELECT COUNT(*) FROM detalle_factura d WHERE d.id_factura = f.id_factura) AS cantidad_lineas "
            + "FROM facturas f JOIN estudiantes e ON e.id_estudiante = f.id_estudiante "
            + "JOIN acudientes a ON a.id_acudiente = f.id_acudiente ";

    private static final RowMapper<Invoice> MAPPER = rs -> new Invoice(
            rs.getInt("id_factura"), rs.getString("numero_factura"), rs.getInt("id_estudiante"), rs.getString("nombre_estudiante"),
            rs.getInt("id_acudiente"), rs.getString("nombre_acudiente"), rs.getObject("fecha_emision", LocalDateTime.class),
            rs.getBigDecimal("subtotal"), rs.getBigDecimal("valor_iva"), rs.getBigDecimal("total"), rs.getString("cufe"),
            rs.getString("resolucion"), rs.getString("nombre_archivo_pdf"), rs.getString("ruta_archivo"),
            rs.getInt("cantidad_lineas"));

    private static final RowMapper<InvoiceLine> LINE_MAPPER = rs -> new InvoiceLine(
            rs.getInt("id_detalle"), rs.getInt("id_factura"), rs.getInt("id_pago"), rs.getString("numero_recibo"),
            rs.getString("descripcion"), rs.getInt("cantidad"), rs.getBigDecimal("valor_unitario"),
            rs.getBigDecimal("porcentaje_iva"), rs.getBigDecimal("valor_iva"), rs.getBigDecimal("total_linea"));

    @Override
    public int insert(Invoice i) {
        return insert("INSERT INTO facturas (numero_factura, id_estudiante, id_acudiente, fecha_emision, subtotal, valor_iva, "
                        + "total, cufe, resolucion, nombre_archivo_pdf, ruta_archivo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                i.number(), i.studentId(), i.guardianId(), i.issuedAt(), i.subtotal(), i.tax(), i.total(), i.cufe(),
                i.resolution(), i.fileName(), i.filePath());
    }

    @Override
    public int insertLine(InvoiceLine l) {
        return insert("INSERT INTO detalle_factura (id_factura, id_pago, descripcion, cantidad, valor_unitario, porcentaje_iva, "
                        + "valor_iva, total_linea) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", l.invoiceId(), l.paymentId(), l.description(),
                l.quantity(), l.unitValue(), l.taxRate(), l.taxAmount(), l.lineTotal());
    }

    @Override
    public Optional<Invoice> findByNumber(String number) {
        return queryOne(SELECT + "WHERE f.numero_factura = ?", MAPPER, number);
    }

    @Override
    public Optional<Invoice> findByPayment(int paymentId) {
        return queryOne(SELECT + "WHERE f.id_factura = (SELECT d.id_factura FROM detalle_factura d WHERE d.id_pago = ?)",
                MAPPER, paymentId);
    }

    @Override
    public List<Invoice> findByStudent(int studentId) {
        return query(SELECT + "WHERE f.id_estudiante = ? ORDER BY f.id_factura DESC", MAPPER, studentId);
    }

    @Override
    public List<Invoice> findAll() {
        return query(SELECT + "ORDER BY f.id_factura DESC", MAPPER);
    }

    @Override
    public List<InvoiceLine> linesOf(int invoiceId) {
        return query("SELECT d.*, p.numero_recibo FROM detalle_factura d JOIN pagos p ON p.id_pago = d.id_pago "
                + "WHERE d.id_factura = ? ORDER BY d.id_detalle", LINE_MAPPER, invoiceId);
    }

    @Override
    public boolean updateFile(int invoiceId, String fileName, String path) {
        return update("UPDATE facturas SET nombre_archivo_pdf = ?, ruta_archivo = ? WHERE id_factura = ?", fileName, path, invoiceId) > 0;
    }
}
