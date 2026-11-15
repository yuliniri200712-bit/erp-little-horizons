package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.StudentAdjustment;
import com.littlehorizons.erp.model.enums.AdjustmentType;

import java.util.List;
import java.util.Optional;

/** Scholarships and discounts per student (ajustes_estudiante). */
public class StudentAdjustmentDao extends BaseDao {

    private static final String SELECT = "SELECT aj.*, c.nombre_concepto FROM ajustes_estudiante aj "
            + "JOIN conceptos_pago c ON c.id_concepto = aj.id_concepto ";

    private static final RowMapper<StudentAdjustment> MAPPER = rs -> new StudentAdjustment(
            rs.getInt("id_ajuste"), rs.getInt("id_estudiante"), rs.getInt("id_concepto"), rs.getString("nombre_concepto"),
            enumValue(rs, "tipo_ajuste", AdjustmentType.class), rs.getBigDecimal("porcentaje"), rs.getString("motivo"),
            rs.getBoolean("esta_activo"));

    public int insert(StudentAdjustment a) {
        return insert("INSERT INTO ajustes_estudiante (id_estudiante, id_concepto, tipo_ajuste, porcentaje, motivo, esta_activo) "
                + "VALUES (?, ?, ?, ?, ?, ?)", a.studentId(), a.conceptId(), a.type(), a.percentage(), a.reason(), a.active());
    }

    public Optional<StudentAdjustment> findById(int id) {
        return queryOne(SELECT + "WHERE aj.id_ajuste = ?", MAPPER, id);
    }

    public List<StudentAdjustment> findByStudent(int studentId) {
        return query(SELECT + "WHERE aj.id_estudiante = ? ORDER BY c.id_concepto", MAPPER, studentId);
    }

    public List<StudentAdjustment> findActive(int studentId, int conceptId) {
        return query(SELECT + "WHERE aj.id_estudiante = ? AND aj.id_concepto = ? AND aj.esta_activo = TRUE", MAPPER, studentId, conceptId);
    }

    public boolean setActive(int id, boolean active) {
        return update("UPDATE ajustes_estudiante SET esta_activo = ? WHERE id_ajuste = ?", active, id) > 0;
    }

    public boolean delete(int id) {
        return update("DELETE FROM ajustes_estudiante WHERE id_ajuste = ?", id) > 0;
    }
}
