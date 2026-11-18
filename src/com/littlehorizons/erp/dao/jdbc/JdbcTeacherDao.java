package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.TeacherStatus;

import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link TeacherDao}: CRUD for teachers (profesores). */
public class JdbcTeacherDao extends BaseDao implements TeacherDao {

    private static final String SELECT = "SELECT p.* FROM profesores p ";
    private static final String ORDER = " ORDER BY p.primer_apellido, p.segundo_apellido, p.primer_nombre";

    private static final RowMapper<Teacher> MAPPER = rs -> new Teacher(
            rs.getInt("id_profesor"), enumValue(rs, "tipo_documento", DocumentType.class), rs.getString("numero_documento"),
            rs.getString("primer_nombre"), rs.getString("segundo_nombre"), rs.getString("primer_apellido"),
            rs.getString("segundo_apellido"), rs.getString("correo_electronico"), rs.getString("telefono"),
            rs.getString("especializacion"), localDate(rs, "fecha_contratacion"), enumValue(rs, "estado", TeacherStatus.class));

    @Override
    public Optional<Teacher> findById(int id) {
        return queryOne(SELECT + "WHERE p.id_profesor = ?", MAPPER, id);
    }

    @Override
    public Optional<Teacher> findByDocument(String documentNumber) {
        return queryOne(SELECT + "WHERE p.numero_documento = ?", MAPPER, documentNumber);
    }

    @Override
    public List<Teacher> findAll() {
        return query(SELECT + ORDER, MAPPER);
    }

    @Override
    public List<Teacher> search(String text) {
        return query(SELECT + "WHERE " + fullNameSql("p") + " LIKE ? OR p.numero_documento LIKE ? OR p.especializacion LIKE ?" + ORDER,
                MAPPER, like(text), like(text), like(text));
    }

    @Override
    public int insert(Teacher t) {
        return insert("INSERT INTO profesores (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido, "
                        + "segundo_apellido, correo_electronico, telefono, especializacion, fecha_contratacion, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                t.documentType(), t.documentNumber(), t.firstName(), t.middleName(), t.lastName(), t.secondLastName(),
                t.email(), t.phone(), t.specialization(), t.hireDate(), t.status());
    }

    @Override
    public boolean update(Teacher t) {
        return update("UPDATE profesores SET tipo_documento = ?, numero_documento = ?, primer_nombre = ?, segundo_nombre = ?, "
                        + "primer_apellido = ?, segundo_apellido = ?, correo_electronico = ?, telefono = ?, especializacion = ?, "
                        + "fecha_contratacion = ?, estado = ? WHERE id_profesor = ?",
                t.documentType(), t.documentNumber(), t.firstName(), t.middleName(), t.lastName(), t.secondLastName(),
                t.email(), t.phone(), t.specialization(), t.hireDate(), t.status(), t.id()) > 0;
    }

    @Override
    public boolean delete(int id) {
        return update("DELETE FROM profesores WHERE id_profesor = ?", id) > 0;
    }

    /** Records that reference the teacher without cascade (grades, attendance, clubs, events). */
    @Override
    public long countBlockingReferences(int teacherId) {
        return count("SELECT (SELECT COUNT(*) FROM registros_academicos WHERE id_profesor = ?) "
                + "+ (SELECT COUNT(*) FROM asistencias WHERE registrado_por = ?) "
                + "+ (SELECT COUNT(*) FROM clubes WHERE id_profesor_supervisor = ?) "
                + "+ (SELECT COUNT(*) FROM eventos WHERE id_organizador = ?)", teacherId, teacherId, teacherId, teacherId);
    }
}
