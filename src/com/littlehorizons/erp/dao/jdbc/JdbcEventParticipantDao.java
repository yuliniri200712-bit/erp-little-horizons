package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.EventParticipantDao;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.enums.ParticipantRole;
import com.littlehorizons.erp.model.enums.Placement;

import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link EventParticipantDao}: Event participants (participantes_evento). */
public class JdbcEventParticipantDao extends BaseDao implements EventParticipantDao {

    private static final String SELECT = "SELECT pe.*, ev.nombre_evento, " + fullNameSql("e") + " AS nombre_estudiante "
            + "FROM participantes_evento pe JOIN eventos ev ON ev.id_evento = pe.id_evento "
            + "JOIN estudiantes e ON e.id_estudiante = pe.id_estudiante ";

    private static final RowMapper<EventParticipant> MAPPER = rs -> new EventParticipant(
            rs.getInt("id_participacion"), rs.getInt("id_evento"), rs.getString("nombre_evento"),
            rs.getInt("id_estudiante"), rs.getString("nombre_estudiante"),
            enumValue(rs, "rol_participacion", ParticipantRole.class),
            enumValue(rs, "posicion_obtenida", Placement.class), rs.getString("observaciones"));

    @Override
    public int insert(EventParticipant p) {
        return insert("INSERT INTO participantes_evento (id_evento, id_estudiante, rol_participacion, posicion_obtenida, observaciones) "
                + "VALUES (?, ?, ?, ?, ?)", p.eventId(), p.studentId(), p.role(), p.placement(), p.notes());
    }

    @Override
    public Optional<EventParticipant> findById(int id) {
        return queryOne(SELECT + "WHERE pe.id_participacion = ?", MAPPER, id);
    }

    @Override
    public List<EventParticipant> findByEvent(int eventId) {
        return query(SELECT + "WHERE pe.id_evento = ? ORDER BY pe.posicion_obtenida IS NULL, pe.posicion_obtenida, e.primer_apellido",
                MAPPER, eventId);
    }

    @Override
    public List<EventParticipant> findByStudent(int studentId) {
        return query(SELECT + "WHERE pe.id_estudiante = ? ORDER BY ev.fecha_evento DESC", MAPPER, studentId);
    }

    @Override
    public boolean exists(int eventId, int studentId) {
        return count("SELECT COUNT(*) FROM participantes_evento WHERE id_evento = ? AND id_estudiante = ?", eventId, studentId) > 0;
    }

    @Override
    public boolean isPlacementTaken(int eventId, Placement placement, int excludeParticipationId) {
        return count("SELECT COUNT(*) FROM participantes_evento WHERE id_evento = ? AND posicion_obtenida = ? "
                + "AND id_participacion <> ?", eventId, placement, excludeParticipationId) > 0;
    }

    @Override
    public boolean updatePlacement(int participationId, Placement placement) {
        return update("UPDATE participantes_evento SET posicion_obtenida = ? WHERE id_participacion = ?", placement, participationId) > 0;
    }

    @Override
    public boolean delete(int id) {
        return update("DELETE FROM participantes_evento WHERE id_participacion = ?", id) > 0;
    }
}
