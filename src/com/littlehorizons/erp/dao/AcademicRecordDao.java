package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.report.CourseAverage;
import com.littlehorizons.erp.model.report.StudentRanking;

import java.util.List;
import java.util.Optional;

/** Grades (registros_academicos) and SQL aggregations for averages. */
public class AcademicRecordDao extends BaseDao {

    private static final String SELECT = "SELECT r.*, m.nombre_materia, " + fullNameSql("p") + " AS nombre_profesor "
            + "FROM registros_academicos r JOIN materias m ON m.id_materia = r.id_materia "
            + "JOIN profesores p ON p.id_profesor = r.id_profesor ";

    private static final RowMapper<AcademicRecord> MAPPER = rs -> new AcademicRecord(
            rs.getInt("id_registro"), rs.getInt("id_estudiante"), rs.getInt("id_materia"), rs.getString("nombre_materia"),
            rs.getInt("id_profesor"), rs.getString("nombre_profesor"), rs.getInt("año_academico"),
            enumValue(rs, "periodo_academico", AcademicPeriod.class), rs.getBigDecimal("valor_nota"),
            rs.getString("observaciones"), localDate(rs, "fecha_registro"));

    public Optional<AcademicRecord> findById(int id) {
        return queryOne(SELECT + "WHERE r.id_registro = ?", MAPPER, id);
    }

    public Optional<AcademicRecord> find(int studentId, int subjectId, int year, AcademicPeriod period) {
        return queryOne(SELECT + "WHERE r.id_estudiante = ? AND r.id_materia = ? AND r.año_academico = ? AND r.periodo_academico = ?",
                MAPPER, studentId, subjectId, year, period);
    }

    public int insert(AcademicRecord r) {
        return insert("INSERT INTO registros_academicos (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, "
                        + "valor_nota, observaciones, fecha_registro) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                r.studentId(), r.subjectId(), r.teacherId(), r.academicYear(), r.period(), r.score(), r.notes(), r.recordDate());
    }

    public boolean update(AcademicRecord r) {
        return update("UPDATE registros_academicos SET id_profesor = ?, valor_nota = ?, observaciones = ?, fecha_registro = ? "
                + "WHERE id_registro = ?", r.teacherId(), r.score(), r.notes(), r.recordDate(), r.id()) > 0;
    }

    public boolean delete(int id) {
        return update("DELETE FROM registros_academicos WHERE id_registro = ?", id) > 0;
    }

    /** All grades of a student; {@code year == null} returns the complete history. */
    public List<AcademicRecord> findByStudent(int studentId, Integer year) {
        return query(SELECT + "WHERE r.id_estudiante = ? AND (? IS NULL OR r.año_academico = ?) "
                + "ORDER BY r.año_academico, m.nombre_materia, r.periodo_academico", MAPPER, studentId, year, year);
    }

    /** Course average per subject of a grade (all students), optionally restricted to one period. */
    public List<CourseAverage> courseAverages(int gradeId, int year, AcademicPeriod period) {
        return query("SELECT m.codigo_materia, m.nombre_materia, g.nombre_grado, COUNT(DISTINCT r.id_estudiante) AS estudiantes, "
                        + "AVG(r.valor_nota) AS promedio, MIN(r.valor_nota) AS minima, MAX(r.valor_nota) AS maxima "
                        + "FROM registros_academicos r JOIN materias m ON m.id_materia = r.id_materia "
                        + "JOIN grados g ON g.id_grado = m.id_grado "
                        + "WHERE m.id_grado = ? AND r.año_academico = ? AND r.periodo_academico <> 'Final' "
                        + "AND (? IS NULL OR r.periodo_academico = ?) "
                        + "GROUP BY m.id_materia, m.codigo_materia, m.nombre_materia, g.nombre_grado ORDER BY m.codigo_materia",
                rs -> new CourseAverage(rs.getString("codigo_materia"), rs.getString("nombre_materia"), rs.getString("nombre_grado"),
                        rs.getLong("estudiantes"), rs.getDouble("promedio"), rs.getBigDecimal("minima"), rs.getBigDecimal("maxima")),
                gradeId, year, period, period);
    }

    /** General average per student (optionally by grade and period), best first. */
    public List<StudentRanking> ranking(Integer gradeId, int year, AcademicPeriod period) {
        return query("SELECT e.id_estudiante, " + fullNameSql("e") + " AS nombre, g.nombre_grado, AVG(r.valor_nota) AS promedio "
                        + "FROM registros_academicos r JOIN estudiantes e ON e.id_estudiante = r.id_estudiante "
                        + "JOIN materias m ON m.id_materia = r.id_materia JOIN grados g ON g.id_grado = m.id_grado "
                        + "WHERE r.año_academico = ? AND r.periodo_academico <> 'Final' AND (? IS NULL OR m.id_grado = ?) "
                        + "AND (? IS NULL OR r.periodo_academico = ?) "
                        + "GROUP BY e.id_estudiante, nombre, g.nombre_grado, g.nivel_numerico "
                        + "ORDER BY g.nivel_numerico, promedio DESC",
                rs -> new StudentRanking(rs.getInt("id_estudiante"), rs.getString("nombre"), rs.getString("nombre_grado"),
                        rs.getDouble("promedio")),
                year, gradeId, gradeId, period, period);
    }
}
