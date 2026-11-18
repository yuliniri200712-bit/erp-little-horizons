package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.enums.ResourceStatus;

import java.util.List;
import java.util.Optional;

/** CRUD for inventory resources (recursos) and their categories (categorias_recurso). */
public interface ResourceDao extends CrudDao<Resource> {

    Optional<Resource> findById(int id);

    Optional<Resource> findByCode(String code);

    List<Resource> findAll();

    List<Resource> findByFilter(Integer categoryId, ResourceStatus status);

    List<Resource> search(String text);

    /** Reads the resource locking its row until the transaction ends. */
    Optional<Resource> findByIdForUpdate(int id);

    int insert(Resource r);

    boolean update(Resource r);

    boolean updateStatus(int id, ResourceStatus status);

    boolean delete(int id);

    List<ResourceCategory> findCategories();

    long countByStatus(ResourceStatus status);
}
