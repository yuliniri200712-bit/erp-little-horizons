package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.model.report.StudentParticipation;

import java.time.LocalDate;
import java.util.List;

/** JDBC implementation of {@link ReportDao}: Cross-module aggregate queries used by the statistics menu. */
public class JdbcReportDao extends BaseDao implements ReportDao {

    @Override
    public long countActiveStudents() {
        return count("SELECT COUNT(*) FROM estudiantes WHERE estado = 'Activo'");
    }

    @Override
    public long countGuardians() {
        return count("SELECT COUNT(*) FROM acudientes");
    }

    @Override
    public long countActiveTeachers() {
        return count("SELECT COUNT(*) FROM profesores WHERE estado = 'Activo'");
    }

    @Override
    public long countActiveSubjects() {
        return count("SELECT COUNT(*) FROM materias WHERE esta_activa = TRUE");
    }

    @Override
    public long countActiveClubs() {
        return count("SELECT COUNT(*) FROM clubes WHERE esta_activo = TRUE");
    }

    @Override
    public long countUpcomingEvents(LocalDate from) {
        return count("SELECT COUNT(*) FROM eventos WHERE fecha_evento >= ? AND esta_activo = TRUE", from);
    }

    @Override
    public long countOverdueLoans() {
        return count("SELECT (SELECT COUNT(*) FROM prestamos_estudiante WHERE estado_prestamo = 'Vencido') "
                + "+ (SELECT COUNT(*) FROM prestamos_profesor WHERE estado_prestamo = 'Vencido')");
    }

    /** Active clubs, events and podium places per student (active students only). */
    @Override
    public List<StudentParticipation> participationByStudent(int year) {
        return query("SELECT e.id_estudiante, " + fullNameSql("e") + " AS nombre, COALESCE(g.nombre_grado, '-') AS grado, "
                        + "(SELECT COUNT(*) FROM membresias_club mc WHERE mc.id_estudiante = e.id_estudiante AND mc.estado_membresia = 'Activa') AS clubes, "
                        + "(SELECT COUNT(*) FROM participantes_evento pe JOIN eventos ev ON ev.id_evento = pe.id_evento "
                        + " WHERE pe.id_estudiante = e.id_estudiante AND YEAR(ev.fecha_evento) = ?) AS eventos, "
                        + "(SELECT COUNT(*) FROM participantes_evento pe JOIN eventos ev ON ev.id_evento = pe.id_evento "
                        + " WHERE pe.id_estudiante = e.id_estudiante AND YEAR(ev.fecha_evento) = ? "
                        + " AND pe.posicion_obtenida IN ('1er lugar', '2do lugar', '3er lugar')) AS podios "
                        + "FROM estudiantes e LEFT JOIN matriculas m ON m.id_estudiante = e.id_estudiante AND m.año_academico = ? "
                        + "LEFT JOIN grados g ON g.id_grado = m.id_grado WHERE e.estado = 'Activo' "
                        + "ORDER BY g.nivel_numerico, e.primer_apellido",
                rs -> new StudentParticipation(rs.getInt("id_estudiante"), rs.getString("nombre"), rs.getString("grado"),
                        rs.getLong("clubes"), rs.getLong("eventos"), rs.getLong("podios")),
                year, year, year);
    }
}
