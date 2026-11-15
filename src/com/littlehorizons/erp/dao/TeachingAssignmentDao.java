package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.TeachingAssignment;
import com.littlehorizons.erp.model.enums.Shift;

import java.util.List;
import java.util.Optional;

/** Teacher-subject assignments per year, section and shift (asignaciones_profesor). */
public class TeachingAssignmentDao extends BaseDao {

    private static final String SELECT = "SELECT a.*, " + fullNameSql("p") + " AS nombre_profesor, m.codigo_materia, "
            + "m.nombre_materia, m.id_grado, g.nombre_grado FROM asignaciones_profesor a "
            + "JOIN profesores p ON p.id_profesor = a.id_profesor JOIN materias m ON m.id_materia = a.id_materia "
            + "JOIN grados g ON g.id_grado = m.id_grado ";
    private static final String ORDER = " ORDER BY a.año_academico DESC, g.nivel_numerico, m.codigo_materia, a.seccion";

    private static final RowMapper<TeachingAssignment> MAPPER = rs -> new TeachingAssignment(
            rs.getInt("id_asignacion"), rs.getInt("id_profesor"), rs.getString("nombre_profesor"), rs.getInt("id_materia"),
            rs.getString("codigo_materia"), rs.getString("nombre_materia"), rs.getInt("id_grado"), rs.getString("nombre_grado"),
            rs.getInt("año_academico"), rs.getString("seccion"), enumValue(rs, "jornada", Shift.class));

    public int insert(TeachingAssignment a) {
        return insert("INSERT INTO asignaciones_profesor (id_profesor, id_materia, año_academico, seccion, jornada) "
                + "VALUES (?, ?, ?, ?, ?)", a.teacherId(), a.subjectId(), a.academicYear(), a.section(), a.shift());
    }

    public Optional<TeachingAssignment> findById(int id) {
        return queryOne(SELECT + "WHERE a.id_asignacion = ?", MAPPER, id);
    }

    public List<TeachingAssignment> findByTeacher(int teacherId, Integer year) {
        return query(SELECT + "WHERE a.id_profesor = ? AND (? IS NULL OR a.año_academico = ?)" + ORDER, MAPPER, teacherId, year, year);
    }

    public List<TeachingAssignment> findByYear(int year) {
        return query(SELECT + "WHERE a.año_academico = ?" + ORDER, MAPPER, year);
    }

    public List<TeachingAssignment> findBySubject(int subjectId, int year) {
        return query(SELECT + "WHERE a.id_materia = ? AND a.año_academico = ?" + ORDER, MAPPER, subjectId, year);
    }

    public boolean isAssigned(int teacherId, int subjectId, int year) {
        return count("SELECT COUNT(*) FROM asignaciones_profesor WHERE id_profesor = ? AND id_materia = ? AND año_academico = ?",
                teacherId, subjectId, year) > 0;
    }

    public boolean delete(int id) {
        return update("DELETE FROM asignaciones_profesor WHERE id_asignacion = ?", id) > 0;
    }
}
