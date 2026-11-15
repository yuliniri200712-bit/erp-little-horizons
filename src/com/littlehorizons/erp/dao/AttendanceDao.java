package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.enums.AttendanceStatus;
import com.littlehorizons.erp.model.report.AbsenceAlert;
import com.littlehorizons.erp.model.report.RecurrentAbsence;

import java.time.LocalDate;
import java.util.List;

/** Daily attendance (asistencias) and absence reports. */
public class AttendanceDao extends BaseDao {

    private static final String SELECT = "SELECT a.*, m.nombre_materia, " + fullNameSql("e") + " AS nombre_estudiante "
            + "FROM asistencias a JOIN materias m ON m.id_materia = a.id_materia "
            + "JOIN estudiantes e ON e.id_estudiante = a.id_estudiante ";

    private static final String GRADE_JOIN = "LEFT JOIN matriculas mt ON mt.id_estudiante = e.id_estudiante "
            + "AND mt.año_academico = YEAR(a.fecha_asistencia) LEFT JOIN grados g ON g.id_grado = mt.id_grado ";

    private static final RowMapper<Attendance> MAPPER = rs -> new Attendance(
            rs.getInt("id_asistencia"), rs.getInt("id_estudiante"), rs.getString("nombre_estudiante"),
            rs.getInt("id_materia"), rs.getString("nombre_materia"), localDate(rs, "fecha_asistencia"),
            enumValue(rs, "estado_asistencia", AttendanceStatus.class), rs.getString("observaciones"),
            nullableInt(rs, "registrado_por"));

    /** Inserts the record or updates it when the student already has attendance for that subject and day. */
    public void upsert(Attendance a) {
        update("INSERT INTO asistencias (id_estudiante, id_materia, fecha_asistencia, estado_asistencia, observaciones, registrado_por) "
                        + "VALUES (?, ?, ?, ?, ?, ?) AS nuevo ON DUPLICATE KEY UPDATE estado_asistencia = nuevo.estado_asistencia, "
                        + "observaciones = nuevo.observaciones, registrado_por = nuevo.registrado_por",
                a.studentId(), a.subjectId(), a.date(), a.status(), a.notes(), a.recordedBy());
    }

    public List<Attendance> findByStudent(int studentId, LocalDate from, LocalDate to) {
        return query(SELECT + "WHERE a.id_estudiante = ? AND a.fecha_asistencia BETWEEN ? AND ? "
                + "ORDER BY a.fecha_asistencia, m.nombre_materia", MAPPER, studentId, from, to);
    }

    public List<Attendance> findBySubjectAndDate(int subjectId, LocalDate date) {
        return query(SELECT + "WHERE a.id_materia = ? AND a.fecha_asistencia = ? ORDER BY e.primer_apellido", MAPPER, subjectId, date);
    }

    /** Students absent on more than {@code maxDays} different days of the same (Monday-based) week. */
    public List<AbsenceAlert> weeklyAbsenceAlerts(LocalDate from, LocalDate to, int maxDays) {
        return query("SELECT e.id_estudiante, " + fullNameSql("e") + " AS nombre, COALESCE(g.nombre_grado, '-') AS grado, "
                        + "DATE_SUB(a.fecha_asistencia, INTERVAL WEEKDAY(a.fecha_asistencia) DAY) AS semana, "
                        + "COUNT(DISTINCT a.fecha_asistencia) AS dias, COUNT(*) AS registros "
                        + "FROM asistencias a JOIN estudiantes e ON e.id_estudiante = a.id_estudiante " + GRADE_JOIN
                        + "WHERE a.estado_asistencia = 'Ausente' AND a.fecha_asistencia BETWEEN ? AND ? "
                        + "GROUP BY e.id_estudiante, nombre, grado, semana HAVING COUNT(DISTINCT a.fecha_asistencia) > ? "
                        + "ORDER BY semana DESC, dias DESC, nombre",
                rs -> new AbsenceAlert(rs.getInt("id_estudiante"), rs.getString("nombre"), rs.getString("grado"),
                        localDate(rs, "semana"), rs.getInt("dias"), rs.getInt("registros")),
                from, to, maxDays);
    }

    /** Students with at least {@code minAbsences} absences in the same subject within the range. */
    public List<RecurrentAbsence> recurrentAbsences(LocalDate from, LocalDate to, int minAbsences) {
        return query("SELECT e.id_estudiante, " + fullNameSql("e") + " AS nombre, COALESCE(g.nombre_grado, '-') AS grado, "
                        + "m.nombre_materia, SUM(a.estado_asistencia = 'Ausente') AS ausencias, "
                        + "SUM(a.estado_asistencia = 'Tardanza') AS tardanzas "
                        + "FROM asistencias a JOIN estudiantes e ON e.id_estudiante = a.id_estudiante "
                        + "JOIN materias m ON m.id_materia = a.id_materia " + GRADE_JOIN
                        + "WHERE a.fecha_asistencia BETWEEN ? AND ? "
                        + "GROUP BY e.id_estudiante, nombre, grado, m.id_materia, m.nombre_materia "
                        + "HAVING SUM(a.estado_asistencia = 'Ausente') >= ? ORDER BY ausencias DESC, nombre",
                rs -> new RecurrentAbsence(rs.getInt("id_estudiante"), rs.getString("nombre"), rs.getString("grado"),
                        rs.getString("nombre_materia"), rs.getInt("ausencias"), rs.getInt("tardanzas")),
                from, to, minAbsences);
    }
}
