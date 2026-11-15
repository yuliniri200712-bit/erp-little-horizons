package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Subject;

import java.util.List;
import java.util.Optional;

/** CRUD for subjects (materias). */
public class SubjectDao extends BaseDao implements CrudDao<Subject> {

    private static final String SELECT = "SELECT m.*, g.nombre_grado FROM materias m JOIN grados g ON g.id_grado = m.id_grado ";
    private static final String ORDER = " ORDER BY g.nivel_numerico, m.codigo_materia";

    private static final RowMapper<Subject> MAPPER = rs -> new Subject(
            rs.getInt("id_materia"), rs.getString("codigo_materia"), rs.getString("nombre_materia"),
            rs.getString("descripcion"), rs.getInt("id_grado"), rs.getString("nombre_grado"),
            rs.getInt("horas_semanales"), rs.getBoolean("esta_activa"));

    @Override
    public Optional<Subject> findById(int id) {
        return queryOne(SELECT + "WHERE m.id_materia = ?", MAPPER, id);
    }

    public Optional<Subject> findByCode(String code) {
        return queryOne(SELECT + "WHERE m.codigo_materia = ?", MAPPER, code);
    }

    @Override
    public List<Subject> findAll() {
        return query(SELECT + ORDER, MAPPER);
    }

    public List<Subject> findByGrade(int gradeId, boolean activeOnly) {
        return query(SELECT + "WHERE m.id_grado = ? AND (? = FALSE OR m.esta_activa = TRUE)" + ORDER, MAPPER, gradeId, activeOnly);
    }

    @Override
    public int insert(Subject s) {
        return insert("INSERT INTO materias (codigo_materia, nombre_materia, descripcion, id_grado, horas_semanales, esta_activa) "
                + "VALUES (?, ?, ?, ?, ?, ?)", s.code(), s.name(), s.description(), s.gradeId(), s.weeklyHours(), s.active());
    }

    @Override
    public boolean update(Subject s) {
        return update("UPDATE materias SET codigo_materia = ?, nombre_materia = ?, descripcion = ?, id_grado = ?, "
                        + "horas_semanales = ?, esta_activa = ? WHERE id_materia = ?",
                s.code(), s.name(), s.description(), s.gradeId(), s.weeklyHours(), s.active(), s.id()) > 0;
    }

    @Override
    public boolean delete(int id) {
        return update("DELETE FROM materias WHERE id_materia = ?", id) > 0;
    }

    public long countDependencies(int subjectId) {
        return count("SELECT (SELECT COUNT(*) FROM registros_academicos WHERE id_materia = ?) "
                + "+ (SELECT COUNT(*) FROM inscripciones_materia WHERE id_materia = ?) "
                + "+ (SELECT COUNT(*) FROM asistencias WHERE id_materia = ?)", subjectId, subjectId, subjectId);
    }
}
