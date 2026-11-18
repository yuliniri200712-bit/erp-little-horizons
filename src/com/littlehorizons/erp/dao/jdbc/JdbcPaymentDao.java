package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.enums.PaymentMethod;
import com.littlehorizons.erp.model.enums.PaymentPeriod;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link PaymentDao}: Payments (pagos) and payment histories. */
public class JdbcPaymentDao extends BaseDao implements PaymentDao {

    private static final String SELECT = "SELECT p.*, cp.nombre_concepto, " + fullNameSql("e") + " AS nombre_estudiante, "
            + fullNameSql("a") + " AS nombre_acudiente FROM pagos p "
            + "JOIN conceptos_pago cp ON cp.id_concepto = p.id_concepto JOIN estudiantes e ON e.id_estudiante = p.id_estudiante "
            + "LEFT JOIN acudientes a ON a.id_acudiente = p.id_acudiente ";

    private static final RowMapper<Payment> MAPPER = rs -> new Payment(
            rs.getInt("id_pago"), rs.getInt("id_estudiante"), rs.getString("nombre_estudiante"),
            nullableInt(rs, "id_acudiente"), rs.getString("nombre_acudiente"), rs.getInt("id_concepto"),
            rs.getString("nombre_concepto"), nullableInt(rs, "id_cargo"), rs.getBigDecimal("monto_pagado"),
            localDate(rs, "fecha_pago"), enumValue(rs, "metodo_pago", PaymentMethod.class), rs.getString("numero_recibo"),
            nullableInt(rs, "año_academico"), enumValue(rs, "periodo_academico", PaymentPeriod.class), rs.getString("notas"));

    @Override
    public int insert(Payment p) {
        return insert("INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, id_cargo, monto_pagado, fecha_pago, metodo_pago, "
                        + "numero_recibo, año_academico, periodo_academico, notas) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                p.studentId(), p.guardianId(), p.conceptId(), p.chargeId(), p.amount(), p.paymentDate(), p.method(),
                p.receiptNumber(), p.academicYear(), p.period(), p.notes());
    }

    @Override
    public Optional<Payment> findById(int id) {
        return queryOne(SELECT + "WHERE p.id_pago = ?", MAPPER, id);
    }

    @Override
    public Optional<Payment> findByReceipt(String receipt) {
        return queryOne(SELECT + "WHERE p.numero_recibo = ?", MAPPER, receipt);
    }

    @Override
    public List<Payment> findByStudent(int studentId) {
        return query(SELECT + "WHERE p.id_estudiante = ? ORDER BY p.fecha_pago DESC, p.id_pago DESC", MAPPER, studentId);
    }

    /** Payments of every student linked to the guardian (covers several children). */
    @Override
    public List<Payment> findByGuardian(int guardianId) {
        return query(SELECT + "WHERE p.id_estudiante IN (SELECT ea.id_estudiante FROM estudiante_acudiente ea WHERE ea.id_acudiente = ?) "
                + "OR p.id_acudiente = ? ORDER BY p.fecha_pago DESC, p.id_pago DESC", MAPPER, guardianId, guardianId);
    }

    @Override
    public List<Payment> findRecent(int limit) {
        return query(SELECT + "ORDER BY p.fecha_pago DESC, p.id_pago DESC LIMIT ?", MAPPER, limit);
    }

    @Override
    public List<Payment> findUnbilledByStudent(int studentId) {
        return query(SELECT + "WHERE p.id_estudiante = ? AND NOT EXISTS (SELECT 1 FROM detalle_factura d WHERE d.id_pago = p.id_pago) "
                + "ORDER BY p.fecha_pago, p.id_pago", MAPPER, studentId);
    }

    @Override
    public boolean updateDetails(int paymentId, PaymentMethod method, String notes) {
        return update("UPDATE pagos SET metodo_pago = ?, notas = ? WHERE id_pago = ?", method, notes, paymentId) > 0;
    }

    @Override
    public boolean delete(int id) {
        return update("DELETE FROM pagos WHERE id_pago = ?", id) > 0;
    }

    @Override
    public BigDecimal totalBetween(LocalDate from, LocalDate to) {
        return sum("SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos WHERE fecha_pago BETWEEN ? AND ?", from, to);
    }
}
