package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;

import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link SubjectEnrollmentDao}: Subject enrollments per period (inscripciones_materia). */
public class JdbcSubjectEnrollmentDao extends BaseDao implements SubjectEnrollmentDao {

    private static final String SELECT = "SELECT i.*, m.nombre_materia, " + fullNameSql("e") + " AS nombre_estudiante "
            + "FROM inscripciones_materia i JOIN materias m ON m.id_materia = i.id_materia "
            + "JOIN estudiantes e ON e.id_estudiante = i.id_estudiante ";

    private static final RowMapper<SubjectEnrollment> MAPPER = rs -> new SubjectEnrollment(
            rs.getInt("id_inscripcion"), rs.getInt("id_estudiante"), rs.getString("nombre_estudiante"),
            rs.getInt("id_materia"), rs.getString("nombre_materia"), rs.getInt("año_academico"),
            enumValue(rs, "periodo_academico", AcademicPeriod.class), rs.getString("seccion"),
            localDate(rs, "fecha_inscripcion"), enumValue(rs, "estado", EnrollmentStatus.class));

    @Override
    public int insert(SubjectEnrollment s) {
        return insert("INSERT INTO inscripciones_materia (id_estudiante, id_materia, año_academico, periodo_academico, seccion, "
                        + "fecha_inscripcion, estado) VALUES (?, ?, ?, ?, ?, ?, ?)",
                s.studentId(), s.subjectId(), s.academicYear(), s.period(), s.section(), s.enrollmentDate(), s.status());
    }

    @Override
    public Optional<SubjectEnrollment> findById(int id) {
        return queryOne(SELECT + "WHERE i.id_inscripcion = ?", MAPPER, id);
    }

    @Override
    public boolean exists(int studentId, int subjectId, int year, AcademicPeriod period) {
        return count("SELECT COUNT(*) FROM inscripciones_materia WHERE id_estudiante = ? AND id_materia = ? "
                + "AND año_academico = ? AND periodo_academico = ?", studentId, subjectId, year, period) > 0;
    }

    /** True if the student has a non-withdrawn enrollment in the subject for the year (any period). */
    @Override
    public boolean isEnrolledInYear(int studentId, int subjectId, int year) {
        return count("SELECT COUNT(*) FROM inscripciones_materia WHERE id_estudiante = ? AND id_materia = ? "
                + "AND año_academico = ? AND estado <> 'Retirada'", studentId, subjectId, year) > 0;
    }

    @Override
    public List<SubjectEnrollment> findByStudent(int studentId, int year) {
        return query(SELECT + "WHERE i.id_estudiante = ? AND i.año_academico = ? ORDER BY i.periodo_academico, m.nombre_materia",
                MAPPER, studentId, year);
    }

    @Override
    public List<SubjectEnrollment> findBySubject(int subjectId, int year, AcademicPeriod period) {
        return query(SELECT + "WHERE i.id_materia = ? AND i.año_academico = ? AND i.periodo_academico = ? AND i.estado <> 'Retirada' "
                + "ORDER BY e.primer_apellido, e.primer_nombre", MAPPER, subjectId, year, period);
    }

    /** Distinct students enrolled (not withdrawn) in a subject during a year. */
    @Override
    public List<SubjectEnrollment> findStudentsOfSubjectInYear(int subjectId, int year) {
        return query(SELECT + "WHERE i.id_materia = ? AND i.año_academico = ? AND i.estado <> 'Retirada' AND i.id_inscripcion = "
                + "(SELECT MAX(x.id_inscripcion) FROM inscripciones_materia x WHERE x.id_estudiante = i.id_estudiante "
                + "AND x.id_materia = i.id_materia AND x.año_academico = i.año_academico AND x.estado <> 'Retirada') "
                + "ORDER BY e.primer_apellido, e.primer_nombre", MAPPER, subjectId, year);
    }

    @Override
    public boolean updateStatus(int id, EnrollmentStatus status) {
        return update("UPDATE inscripciones_materia SET estado = ? WHERE id_inscripcion = ?", status, id) > 0;
    }

    @Override
    public int withdrawAllActive(int studentId, int year) {
        return update("UPDATE inscripciones_materia SET estado = 'Retirada' WHERE id_estudiante = ? AND año_academico = ? "
                + "AND estado = 'Activa'", studentId, year);
    }
}
