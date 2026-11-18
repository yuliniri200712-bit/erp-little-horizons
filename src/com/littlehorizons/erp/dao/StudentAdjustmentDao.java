package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.StudentAdjustment;

import java.util.List;
import java.util.Optional;

/** Scholarships and discounts per student (ajustes_estudiante). */
public interface StudentAdjustmentDao {

    int insert(StudentAdjustment a);

    Optional<StudentAdjustment> findById(int id);

    List<StudentAdjustment> findByStudent(int studentId);

    List<StudentAdjustment> findActive(int studentId, int conceptId);

    boolean setActive(int id, boolean active);

    boolean delete(int id);
}
