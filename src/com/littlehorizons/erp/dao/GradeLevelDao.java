package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.GradeLevel;

import java.util.List;
import java.util.Optional;

/** Read-only access to the grades catalog (grados). */
public interface GradeLevelDao {

    List<GradeLevel> findAll();

    Optional<GradeLevel> findById(int id);
}
