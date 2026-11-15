package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Yearly enrollments (matriculas). */
public class EnrollmentDao extends BaseDao {

    private static final String SELECT = "SELECT m.*, g.nombre_grado, g.nivel_numerico FROM matriculas m "
            + "JOIN grados g ON g.id_grado = m.id_grado ";

    private static final RowMapper<Enrollment> MAPPER = rs -> new Enrollment(
            rs.getInt("id_matricula"), rs.getInt("id_estudiante"), rs.getInt("id_grado"), rs.getString("nombre_grado"),
            rs.getInt("nivel_numerico"), rs.getInt("año_academico"), enumValue(rs, "estado_matricula", EnrollmentStatus.class),
            localDate(rs, "fecha_matricula"), localDate(rs, "fecha_retiro"), rs.getString("observaciones"));

    public int insert(Enrollment e) {
        return insert("INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula, observaciones) "
                + "VALUES (?, ?, ?, ?, ?, ?)", e.studentId(), e.gradeId(), e.academicYear(), e.status(), e.enrollmentDate(), e.notes());
    }

    public Optional<Enrollment> findById(int id) {
        return queryOne(SELECT + "WHERE m.id_matricula = ?", MAPPER, id);
    }

    public List<Enrollment> findByStudent(int studentId) {
        return query(SELECT + "WHERE m.id_estudiante = ? ORDER BY m.año_academico DESC", MAPPER, studentId);
    }

    public Optional<Enrollment> findByStudentAndYear(int studentId, int year) {
        return queryOne(SELECT + "WHERE m.id_estudiante = ? AND m.año_academico = ?", MAPPER, studentId, year);
    }

    public Optional<Enrollment> findLatest(int studentId) {
        return queryOne(SELECT + "WHERE m.id_estudiante = ? ORDER BY m.año_academico DESC LIMIT 1", MAPPER, studentId);
    }

    public List<Enrollment> findByYear(int year, Integer gradeId) {
        return query(SELECT + "WHERE m.año_academico = ? AND (? IS NULL OR m.id_grado = ?) ORDER BY g.nivel_numerico, m.id_estudiante",
                MAPPER, year, gradeId, gradeId);
    }

    public boolean updateStatus(int enrollmentId, EnrollmentStatus status, LocalDate withdrawalDate, String notes) {
        return update("UPDATE matriculas SET estado_matricula = ?, fecha_retiro = ?, observaciones = COALESCE(?, observaciones) "
                + "WHERE id_matricula = ?", status, withdrawalDate, notes, enrollmentId) > 0;
    }

    public boolean delete(int id) {
        return update("DELETE FROM matriculas WHERE id_matricula = ?", id) > 0;
    }
}
