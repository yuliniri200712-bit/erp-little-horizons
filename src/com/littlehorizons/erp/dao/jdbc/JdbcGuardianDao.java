package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.GuardianLink;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.DocumentType;

import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link GuardianDao}: CRUD for guardians (acudientes) and the student-guardian relationship (estudiante_acudiente). */
public class JdbcGuardianDao extends BaseDao implements GuardianDao {

    private static final String COLUMNS = "a.id_acudiente, a.tipo_documento, a.numero_documento, a.primer_nombre, "
            + "a.segundo_nombre, a.primer_apellido, a.segundo_apellido, a.parentesco, a.telefono_principal, "
            + "a.telefono_secundario, a.correo_electronico, a.direccion, a.ocupacion, a.es_responsable_financiero";
    private static final String SELECT = "SELECT " + COLUMNS + " FROM acudientes a ";
    private static final String ORDER = " ORDER BY a.primer_apellido, a.segundo_apellido, a.primer_nombre";

    public static final RowMapper<Guardian> MAPPER = rs -> new Guardian(
            rs.getInt("id_acudiente"),
            enumValue(rs, "tipo_documento", DocumentType.class),
            rs.getString("numero_documento"),
            rs.getString("primer_nombre"),
            rs.getString("segundo_nombre"),
            rs.getString("primer_apellido"),
            rs.getString("segundo_apellido"),
            rs.getString("parentesco"),
            rs.getString("telefono_principal"),
            rs.getString("telefono_secundario"),
            rs.getString("correo_electronico"),
            rs.getString("direccion"),
            rs.getString("ocupacion"),
            rs.getBoolean("es_responsable_financiero"));

    @Override
    public Optional<Guardian> findById(int id) {
        return queryOne(SELECT + "WHERE a.id_acudiente = ?", MAPPER, id);
    }

    @Override
    public Optional<Guardian> findByDocument(String documentNumber) {
        return queryOne(SELECT + "WHERE a.numero_documento = ?", MAPPER, documentNumber);
    }

    @Override
    public List<Guardian> findAll() {
        return query(SELECT + ORDER, MAPPER);
    }

    @Override
    public List<Guardian> search(String text) {
        return query(SELECT + "WHERE " + fullNameSql("a") + " LIKE ? OR a.numero_documento LIKE ?" + ORDER,
                MAPPER, like(text), like(text));
    }

    @Override
    public int insert(Guardian g) {
        return insert("INSERT INTO acudientes (tipo_documento, numero_documento, primer_nombre, segundo_nombre, primer_apellido, "
                        + "segundo_apellido, parentesco, telefono_principal, telefono_secundario, correo_electronico, direccion, "
                        + "ocupacion, es_responsable_financiero) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                g.documentType(), g.documentNumber(), g.firstName(), g.middleName(), g.lastName(), g.secondLastName(),
                g.relationship(), g.mainPhone(), g.secondaryPhone(), g.email(), g.address(), g.occupation(),
                g.financialResponsible());
    }

    @Override
    public boolean update(Guardian g) {
        return update("UPDATE acudientes SET tipo_documento = ?, numero_documento = ?, primer_nombre = ?, segundo_nombre = ?, "
                        + "primer_apellido = ?, segundo_apellido = ?, parentesco = ?, telefono_principal = ?, telefono_secundario = ?, "
                        + "correo_electronico = ?, direccion = ?, ocupacion = ?, es_responsable_financiero = ? WHERE id_acudiente = ?",
                g.documentType(), g.documentNumber(), g.firstName(), g.middleName(), g.lastName(), g.secondLastName(),
                g.relationship(), g.mainPhone(), g.secondaryPhone(), g.email(), g.address(), g.occupation(),
                g.financialResponsible(), g.id()) > 0;
    }

    @Override
    public boolean delete(int id) {
        return update("DELETE FROM acudientes WHERE id_acudiente = ?", id) > 0;
    }

    // ---------------------------------------------------------------- relationship

    @Override
    public void link(int studentId, int guardianId, boolean primary, boolean emergencyContact) {
        update("INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal, es_contacto_emergencia) "
                + "VALUES (?, ?, ?, ?)", studentId, guardianId, primary, emergencyContact);
    }

    @Override
    public void clearPrimary(int studentId) {
        update("UPDATE estudiante_acudiente SET es_principal = FALSE WHERE id_estudiante = ?", studentId);
    }

    @Override
    public long countPayments(int guardianId) {
        return count("SELECT COUNT(*) FROM pagos WHERE id_acudiente = ?", guardianId);
    }

    @Override
    public boolean unlink(int studentId, int guardianId) {
        return update("DELETE FROM estudiante_acudiente WHERE id_estudiante = ? AND id_acudiente = ?", studentId, guardianId) > 0;
    }

    @Override
    public boolean isLinked(int studentId, int guardianId) {
        return count("SELECT COUNT(*) FROM estudiante_acudiente WHERE id_estudiante = ? AND id_acudiente = ?",
                studentId, guardianId) > 0;
    }

    @Override
    public long countGuardiansOf(int studentId) {
        return count("SELECT COUNT(*) FROM estudiante_acudiente WHERE id_estudiante = ?", studentId);
    }

    /** Number of students whose only guardian is the given one. */
    @Override
    public long countStudentsWithOnlyThisGuardian(int guardianId) {
        return count("SELECT COUNT(*) FROM estudiante_acudiente ea WHERE ea.id_acudiente = ? AND "
                + "(SELECT COUNT(*) FROM estudiante_acudiente x WHERE x.id_estudiante = ea.id_estudiante) = 1", guardianId);
    }

    @Override
    public List<GuardianLink> findByStudent(int studentId) {
        return query("SELECT " + COLUMNS + ", ea.es_principal, ea.es_contacto_emergencia FROM acudientes a "
                        + "JOIN estudiante_acudiente ea ON ea.id_acudiente = a.id_acudiente WHERE ea.id_estudiante = ? "
                        + "ORDER BY ea.es_principal DESC, a.primer_apellido",
                rs -> new GuardianLink(MAPPER.map(rs), rs.getBoolean("es_principal"), rs.getBoolean("es_contacto_emergencia")),
                studentId);
    }

    @Override
    public List<Student> findStudentsOf(int guardianId) {
        return query("SELECT e.* FROM estudiantes e JOIN estudiante_acudiente ea ON ea.id_estudiante = e.id_estudiante "
                + "WHERE ea.id_acudiente = ? ORDER BY e.fecha_nacimiento", JdbcStudentDao.MAPPER, guardianId);
    }

    /** Guardian responsible for payments: primary and financially responsible first. */
    @Override
    public Optional<Guardian> findFinancialGuardian(int studentId) {
        return queryOne("SELECT " + COLUMNS + " FROM acudientes a JOIN estudiante_acudiente ea ON ea.id_acudiente = a.id_acudiente "
                + "WHERE ea.id_estudiante = ? ORDER BY a.es_responsable_financiero DESC, ea.es_principal DESC LIMIT 1", MAPPER, studentId);
    }
}
