package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.EmergencyContact;

import java.util.List;
import java.util.Optional;

/** Additional emergency contacts of students (contactos_emergencia). */
public class EmergencyContactDao extends BaseDao {

    private static final RowMapper<EmergencyContact> MAPPER = rs -> new EmergencyContact(
            rs.getInt("id_contacto"), rs.getInt("id_estudiante"), rs.getString("nombre_completo"),
            rs.getString("parentesco"), rs.getString("telefono"), rs.getString("telefono_alterno"),
            rs.getString("observaciones"));

    public int insert(EmergencyContact c) {
        return insert("INSERT INTO contactos_emergencia (id_estudiante, nombre_completo, parentesco, telefono, telefono_alterno, "
                + "observaciones) VALUES (?, ?, ?, ?, ?, ?)", c.studentId(), c.fullName(), c.relationship(), c.phone(),
                c.alternatePhone(), c.notes());
    }

    public Optional<EmergencyContact> findById(int id) {
        return queryOne("SELECT * FROM contactos_emergencia WHERE id_contacto = ?", MAPPER, id);
    }

    public List<EmergencyContact> findByStudent(int studentId) {
        return query("SELECT * FROM contactos_emergencia WHERE id_estudiante = ? ORDER BY id_contacto", MAPPER, studentId);
    }

    public boolean delete(int id) {
        return update("DELETE FROM contactos_emergencia WHERE id_contacto = ?", id) > 0;
    }
}
