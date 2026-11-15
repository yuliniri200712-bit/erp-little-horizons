package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.enums.MembershipStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Club memberships (membresias_club). */
public class ClubMembershipDao extends BaseDao {

    private static final String SELECT = "SELECT mc.*, c.nombre_club, " + fullNameSql("e") + " AS nombre_estudiante "
            + "FROM membresias_club mc JOIN clubes c ON c.id_club = mc.id_club "
            + "JOIN estudiantes e ON e.id_estudiante = mc.id_estudiante ";

    private static final RowMapper<ClubMembership> MAPPER = rs -> new ClubMembership(
            rs.getInt("id_membresia"), rs.getInt("id_estudiante"), rs.getString("nombre_estudiante"), rs.getInt("id_club"),
            rs.getString("nombre_club"), localDate(rs, "fecha_ingreso"), enumValue(rs, "estado_membresia", MembershipStatus.class),
            localDate(rs, "fecha_retiro"), rs.getString("motivo_retiro"));

    public int insert(int studentId, int clubId, LocalDate joinDate) {
        return insert("INSERT INTO membresias_club (id_estudiante, id_club, fecha_ingreso, estado_membresia) VALUES (?, ?, ?, 'Activa')",
                studentId, clubId, joinDate);
    }

    public Optional<ClubMembership> findById(int id) {
        return queryOne(SELECT + "WHERE mc.id_membresia = ?", MAPPER, id);
    }

    public List<ClubMembership> findByClub(int clubId, boolean activeOnly) {
        return query(SELECT + "WHERE mc.id_club = ? AND (? = FALSE OR mc.estado_membresia = 'Activa') "
                + "ORDER BY mc.estado_membresia, e.primer_apellido", MAPPER, clubId, activeOnly);
    }

    public List<ClubMembership> findByStudent(int studentId) {
        return query(SELECT + "WHERE mc.id_estudiante = ? ORDER BY mc.fecha_ingreso DESC", MAPPER, studentId);
    }

    public long countActive(int clubId) {
        return count("SELECT COUNT(*) FROM membresias_club WHERE id_club = ? AND estado_membresia = 'Activa'", clubId);
    }

    public boolean hasActive(int studentId, int clubId) {
        return count("SELECT COUNT(*) FROM membresias_club WHERE id_estudiante = ? AND id_club = ? AND estado_membresia = 'Activa'",
                studentId, clubId) > 0;
    }

    public boolean withdraw(int membershipId, LocalDate date, String reason) {
        return update("UPDATE membresias_club SET estado_membresia = 'Retirada', fecha_retiro = ?, motivo_retiro = ? "
                + "WHERE id_membresia = ? AND estado_membresia = 'Activa'", date, reason, membershipId) > 0;
    }

    public int withdrawAllOfStudent(int studentId, LocalDate date, String reason) {
        return update("UPDATE membresias_club SET estado_membresia = 'Retirada', fecha_retiro = ?, motivo_retiro = ? "
                + "WHERE id_estudiante = ? AND estado_membresia = 'Activa'", date, reason, studentId);
    }
}
