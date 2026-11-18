package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.Gender;
import com.littlehorizons.erp.model.enums.StudentStatus;

import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link StudentDao}: CRUD and queries for students (estudiantes). */
public class JdbcStudentDao extends BaseDao implements StudentDao {

    private static final String COLUMNS = "e.id_estudiante, e.tipo_documento, e.numero_documento, e.primer_nombre, "
            + "e.segundo_nombre, e.primer_apellido, e.segundo_apellido, e.fecha_nacimiento, e.genero, e.direccion, "
            + "e.telefono, e.correo_electronico, e.tipo_sangre, e.alergias, e.condiciones_medicas, e.fecha_ingreso, e.estado";
    private static final String SELECT = "SELECT " + COLUMNS + " FROM estudiantes e ";
    private static final String ORDER = " ORDER BY e.primer_apellido, e.segundo_apellido, e.primer_nombre";

    public static final RowMapper<Student> MAPPER = rs -> new Student(
            rs.getInt("id_estudiante"),
            enumValue(rs, "tipo_documento", DocumentType.class),
            rs.getString("numero_documento"),
            rs.getString("primer_nombre"),
            rs.getString("segundo_nombre"),
            rs.getString("primer_apellido"),
            rs.getString("segundo_apellido"),
            localDate(rs, "fecha_nacimiento"),
            enumValue(rs, "genero", Gender.class),
            rs.getString("direccion"),
            rs.getString("telefono"),
            rs.getString("correo_electronico"),
            rs.getString("tipo_sangre"),
            rs.getString("alergias"),
            rs.getString("condiciones_medicas"),
            localDate(rs, "fecha_ingreso"),
            enumValue(rs, "estado", StudentStatus.class));

    @Override
    public Optional<Student> findById(int id) {
        return queryOne(SELECT + "WHERE e.id_estudiante = ?", MAPPER, id);
    }

    @Override
    public Optional<Student> findByDocument(String documentNumber) {
        return queryOne(SELECT + "WHERE e.numero_documento = ?", MAPPER, documentNumber);
    }

    @Override
    public List<Student> findAll() {
        return query(SELECT + ORDER, MAPPER);
    }

    @Override
    public List<Student> findByStatus(StudentStatus status) {
        return query(SELECT + "WHERE e.estado = ?" + ORDER, MAPPER, status);
    }

    @Override
    public List<Student> search(String text) {
        return query(SELECT + "WHERE " + fullNameSql("e") + " LIKE ? OR e.numero_documento LIKE ?" + ORDER,
                MAPPER, like(text), like(text));
    }

    /** Students with an active enrollment in the given grade and year. */
    @Override
    public List<Student> findEnrolled(int gradeId, int year) {
        return query(SELECT + "JOIN matriculas m ON m.id_estudiante = e.id_estudiante "
                + "WHERE m.id_grado = ? AND m.año_academico = ? AND m.estado_matricula = 'Activa'" + ORDER, MAPPER, gradeId, year);
    }

    @Override
    public int insert(Student s) {
        return insert("INSERT INTO estudiantes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, "
                        + "primer_apellido, segundo_apellido, fecha_nacimiento, genero, direccion, telefono, correo_electronico, "
                        + "tipo_sangre, alergias, condiciones_medicas, fecha_ingreso, estado) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                s.documentType(), s.documentNumber(), s.firstName(), s.middleName(), s.lastName(), s.secondLastName(),
                s.birthDate(), s.gender(), s.address(), s.phone(), s.email(), s.bloodType(), s.allergies(),
                s.medicalConditions(), s.admissionDate(), s.status());
    }

    @Override
    public boolean update(Student s) {
        return update("UPDATE estudiantes SET tipo_documento = ?, numero_documento = ?, primer_nombre = ?, segundo_nombre = ?, "
                        + "primer_apellido = ?, segundo_apellido = ?, fecha_nacimiento = ?, genero = ?, direccion = ?, telefono = ?, "
                        + "correo_electronico = ?, tipo_sangre = ?, alergias = ?, condiciones_medicas = ?, fecha_ingreso = ? "
                        + "WHERE id_estudiante = ?",
                s.documentType(), s.documentNumber(), s.firstName(), s.middleName(), s.lastName(), s.secondLastName(),
                s.birthDate(), s.gender(), s.address(), s.phone(), s.email(), s.bloodType(), s.allergies(),
                s.medicalConditions(), s.admissionDate(), s.id()) > 0;
    }

    @Override
    public boolean updateStatus(int studentId, StudentStatus status) {
        return update("UPDATE estudiantes SET estado = ? WHERE id_estudiante = ?", status, studentId) > 0;
    }

    @Override
    public boolean delete(int id) {
        return update("DELETE FROM estudiantes WHERE id_estudiante = ?", id) > 0;
    }

    @Override
    public long countPayments(int studentId) {
        return count("SELECT COUNT(*) FROM pagos WHERE id_estudiante = ?", studentId);
    }
}
