package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.enums.ResourceStatus;

import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link ResourceDao}: CRUD for inventory resources (recursos) and their categories (categorias_recurso). */
public class JdbcResourceDao extends BaseDao implements ResourceDao {

    private static final String SELECT = "SELECT r.*, c.nombre_categoria FROM recursos r "
            + "JOIN categorias_recurso c ON c.id_categoria = r.id_categoria ";
    private static final String ORDER = " ORDER BY r.codigo_recurso";

    private static final RowMapper<Resource> MAPPER = rs -> new Resource(
            rs.getInt("id_recurso"), rs.getString("codigo_recurso"), rs.getString("nombre_recurso"), rs.getInt("id_categoria"),
            rs.getString("nombre_categoria"), rs.getString("descripcion"), localDate(rs, "fecha_adquisicion"),
            rs.getString("ubicacion_fisica"), enumValue(rs, "estado_recurso", ResourceStatus.class));

    @Override
    public Optional<Resource> findById(int id) {
        return queryOne(SELECT + "WHERE r.id_recurso = ?", MAPPER, id);
    }

    @Override
    public Optional<Resource> findByCode(String code) {
        return queryOne(SELECT + "WHERE r.codigo_recurso = ?", MAPPER, code);
    }

    @Override
    public List<Resource> findAll() {
        return query(SELECT + ORDER, MAPPER);
    }

    @Override
    public List<Resource> findByFilter(Integer categoryId, ResourceStatus status) {
        return query(SELECT + "WHERE (? IS NULL OR r.id_categoria = ?) AND (? IS NULL OR r.estado_recurso = ?)" + ORDER,
                MAPPER, categoryId, categoryId, status, status);
    }

    @Override
    public List<Resource> search(String text) {
        return query(SELECT + "WHERE r.codigo_recurso LIKE ? OR r.nombre_recurso LIKE ?" + ORDER, MAPPER, like(text), like(text));
    }

    /** Reads the resource locking its row until the transaction ends. */
    @Override
    public Optional<Resource> findByIdForUpdate(int id) {
        return queryOne(SELECT + "WHERE r.id_recurso = ? FOR UPDATE", MAPPER, id);
    }

    @Override
    public int insert(Resource r) {
        return insert("INSERT INTO recursos (codigo_recurso, nombre_recurso, id_categoria, descripcion, fecha_adquisicion, "
                        + "ubicacion_fisica, estado_recurso) VALUES (?, ?, ?, ?, ?, ?, ?)", r.code(), r.name(), r.categoryId(),
                r.description(), r.acquisitionDate(), r.location(), r.status());
    }

    @Override
    public boolean update(Resource r) {
        return update("UPDATE recursos SET codigo_recurso = ?, nombre_recurso = ?, id_categoria = ?, descripcion = ?, "
                        + "fecha_adquisicion = ?, ubicacion_fisica = ? WHERE id_recurso = ?", r.code(), r.name(), r.categoryId(),
                r.description(), r.acquisitionDate(), r.location(), r.id()) > 0;
    }

    @Override
    public boolean updateStatus(int id, ResourceStatus status) {
        return update("UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?", status, id) > 0;
    }

    @Override
    public boolean delete(int id) {
        return update("DELETE FROM recursos WHERE id_recurso = ?", id) > 0;
    }

    @Override
    public List<ResourceCategory> findCategories() {
        return query("SELECT * FROM categorias_recurso ORDER BY id_categoria",
                rs -> new ResourceCategory(rs.getInt("id_categoria"), rs.getString("nombre_categoria"), rs.getString("descripcion")));
    }

    @Override
    public long countByStatus(ResourceStatus status) {
        return count("SELECT COUNT(*) FROM recursos WHERE estado_recurso = ?", status);
    }
}
