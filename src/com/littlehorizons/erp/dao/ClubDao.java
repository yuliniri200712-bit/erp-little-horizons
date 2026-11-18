package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;

import java.util.List;
import java.util.Optional;

/** CRUD for clubs (clubes) including the count of active members. */
public interface ClubDao extends CrudDao<Club> {

    Optional<Club> findById(int id);

    List<Club> findAll();

    /** Locks the club row until the end of the current transaction (serializes seat allocation). */
    void lockForUpdate(int clubId);

    int insert(Club c);

    boolean update(Club c);

    boolean delete(int id);
}
