package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.ClassScheduleDao;
import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.enums.WeekDay;

import java.time.LocalTime;
import java.util.List;

/** JDBC implementation of {@link ClassScheduleDao}: Class blocks (horarios_clase) and overlap detection. */
public class JdbcClassScheduleDao extends BaseDao implements ClassScheduleDao {

    private static final String SELECT = "SELECT h.*, " + fullNameSql("p") + " AS nombre_profesor, m.nombre_materia, "
            + "g.nombre_grado, a.seccion FROM horarios_clase h "
            + "JOIN asignaciones_profesor a ON a.id_asignacion = h.id_asignacion "
            + "JOIN profesores p ON p.id_profesor = a.id_profesor JOIN materias m ON m.id_materia = a.id_materia "
            + "JOIN grados g ON g.id_grado = m.id_grado ";
    private static final String ORDER = " ORDER BY FIELD(h.dia_semana, 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'), "
            + "h.hora_inicio";

    private static final RowMapper<ClassSchedule> MAPPER = rs -> new ClassSchedule(
            rs.getInt("id_horario"), rs.getInt("id_asignacion"), enumValue(rs, "dia_semana", WeekDay.class),
            localTime(rs, "hora_inicio"), localTime(rs, "hora_fin"), rs.getString("aula"), rs.getString("nombre_profesor"),
            rs.getString("nombre_materia"), rs.getString("nombre_grado"), rs.getString("seccion"));

    @Override
    public int insert(ClassSchedule s) {
        return insert("INSERT INTO horarios_clase (id_asignacion, dia_semana, hora_inicio, hora_fin, aula) VALUES (?, ?, ?, ?, ?)",
                s.assignmentId(), s.day(), s.startTime(), s.endTime(), s.classroom());
    }

    @Override
    public boolean delete(int id) {
        return update("DELETE FROM horarios_clase WHERE id_horario = ?", id) > 0;
    }

    @Override
    public List<ClassSchedule> findByTeacher(int teacherId, int year) {
        return query(SELECT + "WHERE a.id_profesor = ? AND a.año_academico = ?" + ORDER, MAPPER, teacherId, year);
    }

    @Override
    public List<ClassSchedule> findByGroup(int gradeId, String section, int year) {
        return query(SELECT + "WHERE m.id_grado = ? AND a.seccion = ? AND a.año_academico = ?" + ORDER, MAPPER, gradeId, section, year);
    }

    @Override
    public List<ClassSchedule> findByAssignment(int assignmentId) {
        return query(SELECT + "WHERE h.id_asignacion = ?" + ORDER, MAPPER, assignmentId);
    }

    /**
     * Blocks of the same year and day that overlap [start, end) and share the teacher, the group
     * (grade + section) or the classroom.
     */
    @Override
    public List<ClassSchedule> findOverlapping(int year, WeekDay day, LocalTime start, LocalTime end,
                                               int teacherId, int gradeId, String section, String classroom) {
        return query(SELECT + "WHERE a.año_academico = ? AND h.dia_semana = ? AND h.hora_inicio < ? AND ? < h.hora_fin "
                        + "AND (a.id_profesor = ? OR (m.id_grado = ? AND a.seccion = ?) OR (? IS NOT NULL AND h.aula = ?))" + ORDER,
                MAPPER, year, day, end, start, teacherId, gradeId, section, classroom, classroom);
    }
}
