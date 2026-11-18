package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.ChargeDao;
import com.littlehorizons.erp.model.Charge;
import com.littlehorizons.erp.model.enums.ChargeStatus;
import com.littlehorizons.erp.model.report.DebtRow;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link ChargeDao}: Receivables (cargos), paid amounts and the arrears report. */
public class JdbcChargeDao extends BaseDao implements ChargeDao {

    private static final String PAID = "(SELECT COALESCE(SUM(pg.monto_pagado), 0) FROM pagos pg WHERE pg.id_cargo = c.id_cargo)";
    private static final String SELECT = "SELECT c.*, cp.nombre_concepto, " + fullNameSql("e") + " AS nombre_estudiante, "
            + PAID + " AS pagado FROM cargos c JOIN conceptos_pago cp ON cp.id_concepto = c.id_concepto "
            + "JOIN estudiantes e ON e.id_estudiante = c.id_estudiante ";

    private static final RowMapper<Charge> MAPPER = rs -> new Charge(
            rs.getInt("id_cargo"), rs.getInt("id_estudiante"), rs.getString("nombre_estudiante"), rs.getInt("id_concepto"),
            rs.getString("nombre_concepto"), rs.getString("descripcion"), rs.getBigDecimal("monto"), rs.getBigDecimal("pagado"),
            localDate(rs, "fecha_emision"), localDate(rs, "fecha_vencimiento"), enumValue(rs, "estado_cargo", ChargeStatus.class));

    @Override
    public int insert(Charge c) {
        return insert("INSERT INTO cargos (id_estudiante, id_concepto, descripcion, monto, fecha_emision, fecha_vencimiento, estado_cargo) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)", c.studentId(), c.conceptId(), c.description(), c.amount(), c.issueDate(),
                c.dueDate(), c.status());
    }

    @Override
    public Optional<Charge> findById(int id) {
        return queryOne(SELECT + "WHERE c.id_cargo = ?", MAPPER, id);
    }

    @Override
    public void lockForUpdate(int chargeId) {
        query("SELECT id_cargo FROM cargos WHERE id_cargo = ? FOR UPDATE", rs -> rs.getInt(1), chargeId);
    }

    @Override
    public List<Charge> findByStudent(int studentId, boolean pendingOnly) {
        return query(SELECT + "WHERE c.id_estudiante = ? AND (? = FALSE OR c.estado_cargo = 'Pendiente') "
                + "ORDER BY c.fecha_vencimiento DESC", MAPPER, studentId, pendingOnly);
    }

    @Override
    public boolean existsDuplicate(int studentId, int conceptId, String description) {
        return count("SELECT COUNT(*) FROM cargos WHERE id_estudiante = ? AND id_concepto = ? AND descripcion = ? "
                + "AND estado_cargo <> 'Anulado'", studentId, conceptId, description) > 0;
    }

    @Override
    public boolean updateStatus(int chargeId, ChargeStatus status) {
        return update("UPDATE cargos SET estado_cargo = ? WHERE id_cargo = ?", status, chargeId) > 0;
    }

    @Override
    public BigDecimal paidAmount(int chargeId) {
        return sum("SELECT COALESCE(SUM(monto_pagado), 0) FROM pagos WHERE id_cargo = ?", chargeId);
    }

    /** Pending charges whose due date is before {@code cutoff} (e.g. today minus one month). */
    @Override
    public List<DebtRow> overdueBefore(LocalDate cutoff) {
        return query("SELECT c.id_cargo, e.id_estudiante, " + fullNameSql("e") + " AS nombre_estudiante, "
                        + "COALESCE(g.nombre_grado, '-') AS grado, "
                        + "(SELECT " + fullNameSql("a") + " FROM acudientes a JOIN estudiante_acudiente ea ON ea.id_acudiente = a.id_acudiente "
                        + " WHERE ea.id_estudiante = e.id_estudiante ORDER BY a.es_responsable_financiero DESC, ea.es_principal DESC LIMIT 1) AS acudiente, "
                        + "(SELECT a.telefono_principal FROM acudientes a JOIN estudiante_acudiente ea ON ea.id_acudiente = a.id_acudiente "
                        + " WHERE ea.id_estudiante = e.id_estudiante ORDER BY a.es_responsable_financiero DESC, ea.es_principal DESC LIMIT 1) AS telefono, "
                        + "cp.nombre_concepto, c.descripcion, c.monto, " + PAID + " AS pagado, c.fecha_vencimiento, "
                        + "DATEDIFF(CURDATE(), c.fecha_vencimiento) AS dias "
                        + "FROM cargos c JOIN estudiantes e ON e.id_estudiante = c.id_estudiante "
                        + "JOIN conceptos_pago cp ON cp.id_concepto = c.id_concepto "
                        + "LEFT JOIN matriculas m ON m.id_estudiante = e.id_estudiante AND m.año_academico = YEAR(c.fecha_vencimiento) "
                        + "LEFT JOIN grados g ON g.id_grado = m.id_grado "
                        + "WHERE c.estado_cargo = 'Pendiente' AND c.fecha_vencimiento < ? "
                        + "ORDER BY dias DESC, nombre_estudiante",
                rs -> new DebtRow(rs.getInt("id_cargo"), rs.getInt("id_estudiante"), rs.getString("nombre_estudiante"),
                        rs.getString("grado"), rs.getString("acudiente"), rs.getString("telefono"), rs.getString("nombre_concepto"),
                        rs.getString("descripcion"), rs.getBigDecimal("monto"), rs.getBigDecimal("pagado"),
                        localDate(rs, "fecha_vencimiento"), rs.getLong("dias")),
                cutoff);
    }
}
