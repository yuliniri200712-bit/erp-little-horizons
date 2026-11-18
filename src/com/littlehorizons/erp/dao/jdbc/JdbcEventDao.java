package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.enums.EventType;
import com.littlehorizons.erp.model.report.EventSummary;

import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link EventDao}: CRUD for school events (eventos). */
public class JdbcEventDao extends BaseDao implements EventDao {

    private static final String SELECT = "SELECT ev.*, " + fullNameSql("p") + " AS nombre_organizador FROM eventos ev "
            + "LEFT JOIN profesores p ON p.id_profesor = ev.id_organizador ";

    private static final RowMapper<SchoolEvent> MAPPER = rs -> new SchoolEvent(
            rs.getInt("id_evento"), rs.getString("nombre_evento"), rs.getString("descripcion"),
            enumValue(rs, "tipo_evento", EventType.class), localDate(rs, "fecha_evento"), rs.getString("lugar"),
            rs.getBoolean("tiene_competencia"), nullableInt(rs, "id_organizador"), rs.getString("nombre_organizador"),
            rs.getBoolean("esta_activo"));

    @Override
    public Optional<SchoolEvent> findById(int id) {
        return queryOne(SELECT + "WHERE ev.id_evento = ?", MAPPER, id);
    }

    @Override
    public List<SchoolEvent> findAll() {
        return query(SELECT + "ORDER BY ev.fecha_evento DESC", MAPPER);
    }

    @Override
    public int insert(SchoolEvent e) {
        return insert("INSERT INTO eventos (nombre_evento, descripcion, tipo_evento, fecha_evento, lugar, tiene_competencia, "
                        + "id_organizador, esta_activo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", e.name(), e.description(), e.type(),
                e.date(), e.place(), e.hasCompetition(), e.organizerId(), e.active());
    }

    @Override
    public boolean update(SchoolEvent e) {
        return update("UPDATE eventos SET nombre_evento = ?, descripcion = ?, tipo_evento = ?, fecha_evento = ?, lugar = ?, "
                        + "tiene_competencia = ?, id_organizador = ?, esta_activo = ? WHERE id_evento = ?", e.name(), e.description(),
                e.type(), e.date(), e.place(), e.hasCompetition(), e.organizerId(), e.active(), e.id()) > 0;
    }

    @Override
    public boolean delete(int id) {
        return update("DELETE FROM eventos WHERE id_evento = ?", id) > 0;
    }

    /** Events with number of participants and the podium (1st, 2nd, 3rd) as text. */
    @Override
    public List<EventSummary> summaries(int year) {
        return query("SELECT ev.id_evento, ev.nombre_evento, ev.tipo_evento, ev.fecha_evento, ev.tiene_competencia, "
                        + "COUNT(pe.id_participacion) AS participantes, "
                        + "GROUP_CONCAT(CASE WHEN pe.posicion_obtenida IN ('1er lugar', '2do lugar', '3er lugar') "
                        + "THEN CONCAT(pe.posicion_obtenida, ': ', e.primer_nombre, ' ', e.primer_apellido) END "
                        + "ORDER BY pe.posicion_obtenida SEPARATOR ' | ') AS podio "
                        + "FROM eventos ev LEFT JOIN participantes_evento pe ON pe.id_evento = ev.id_evento "
                        + "LEFT JOIN estudiantes e ON e.id_estudiante = pe.id_estudiante WHERE YEAR(ev.fecha_evento) = ? "
                        + "GROUP BY ev.id_evento, ev.nombre_evento, ev.tipo_evento, ev.fecha_evento, ev.tiene_competencia "
                        + "ORDER BY ev.fecha_evento",
                rs -> new EventSummary(rs.getInt("id_evento"), rs.getString("nombre_evento"),
                        enumValue(rs, "tipo_evento", EventType.class), localDate(rs, "fecha_evento"),
                        rs.getBoolean("tiene_competencia"), rs.getLong("participantes"), rs.getString("podio")),
                year);
    }
}
