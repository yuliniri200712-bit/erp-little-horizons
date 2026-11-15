package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;

import java.util.List;
import java.util.Optional;

/** CRUD for clubs (clubes) including the count of active members. */
public class ClubDao extends BaseDao implements CrudDao<Club> {

    private static final String SELECT = "SELECT c.*, " + fullNameSql("p") + " AS nombre_supervisor, "
            + "(SELECT COUNT(*) FROM membresias_club mc WHERE mc.id_club = c.id_club AND mc.estado_membresia = 'Activa') AS activos "
            + "FROM clubes c LEFT JOIN profesores p ON p.id_profesor = c.id_profesor_supervisor ";

    private static final RowMapper<Club> MAPPER = rs -> new Club(
            rs.getInt("id_club"), rs.getString("nombre_club"), rs.getString("descripcion"),
            nullableInt(rs, "id_profesor_supervisor"), rs.getString("nombre_supervisor"), rs.getInt("capacidad_maxima"),
            rs.getString("horario_reunion"), rs.getBoolean("esta_activo"), rs.getInt("activos"));

    @Override
    public Optional<Club> findById(int id) {
        return queryOne(SELECT + "WHERE c.id_club = ?", MAPPER, id);
    }

    @Override
    public List<Club> findAll() {
        return query(SELECT + "ORDER BY c.nombre_club", MAPPER);
    }

    /** Locks the club row until the end of the current transaction (serializes seat allocation). */
    public void lockForUpdate(int clubId) {
        query("SELECT id_club FROM clubes WHERE id_club = ? FOR UPDATE", rs -> rs.getInt(1), clubId);
    }

    @Override
    public int insert(Club c) {
        return insert("INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, capacidad_maxima, horario_reunion, "
                + "esta_activo) VALUES (?, ?, ?, ?, ?, ?)", c.name(), c.description(), c.supervisorId(), c.maxCapacity(),
                c.meetingSchedule(), c.active());
    }

    @Override
    public boolean update(Club c) {
        return update("UPDATE clubes SET nombre_club = ?, descripcion = ?, id_profesor_supervisor = ?, capacidad_maxima = ?, "
                        + "horario_reunion = ?, esta_activo = ? WHERE id_club = ?", c.name(), c.description(), c.supervisorId(),
                c.maxCapacity(), c.meetingSchedule(), c.active(), c.id()) > 0;
    }

    @Override
    public boolean delete(int id) {
        return update("DELETE FROM clubes WHERE id_club = ?", id) > 0;
    }
}
