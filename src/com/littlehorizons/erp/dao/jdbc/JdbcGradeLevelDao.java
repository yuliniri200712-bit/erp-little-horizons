package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.GradeLevelDao;
import com.littlehorizons.erp.model.GradeLevel;

import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link GradeLevelDao}: Read-only access to the grades catalog (grados). */
public class JdbcGradeLevelDao extends BaseDao implements GradeLevelDao {

    private static final RowMapper<GradeLevel> MAPPER = rs ->
            new GradeLevel(rs.getInt("id_grado"), rs.getString("nombre_grado"), rs.getInt("nivel_numerico"));

    @Override
    public List<GradeLevel> findAll() {
        return query("SELECT id_grado, nombre_grado, nivel_numerico FROM grados ORDER BY nivel_numerico", MAPPER);
    }

    @Override
    public Optional<GradeLevel> findById(int id) {
        return queryOne("SELECT id_grado, nombre_grado, nivel_numerico FROM grados WHERE id_grado = ?", MAPPER, id);
    }
}
