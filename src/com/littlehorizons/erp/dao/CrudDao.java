package com.littlehorizons.erp.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD contract implemented by the DAOs of the main entities
 * (Interface Segregation: read-only catalogs do not implement it).
 *
 * @param <T> entity type
 */
public interface CrudDao<T> {

    Optional<T> findById(int id);

    List<T> findAll();

    /** Inserts the entity and returns its generated id. */
    int insert(T entity);

    boolean update(T entity);

    boolean delete(int id);
}
