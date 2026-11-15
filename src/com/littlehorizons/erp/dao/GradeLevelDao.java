package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.GradeLevel;

import java.util.List;
import java.util.Optional;

/** Read-only access to the grades catalog (grados). */
public class GradeLevelDao extends BaseDao {

    private static final RowMapper<GradeLevel> MAPPER = rs ->
            new GradeLevel(rs.getInt("id_grado"), rs.getString("nombre_grado"), rs.getInt("nivel_numerico"));

    public List<GradeLevel> findAll() {
        return query("SELECT id_grado, nombre_grado, nivel_numerico FROM grados ORDER BY nivel_numerico", MAPPER);
    }

    public Optional<GradeLevel> findById(int id) {
        return queryOne("SELECT id_grado, nombre_grado, nivel_numerico FROM grados WHERE id_grado = ?", MAPPER, id);
    }
}
