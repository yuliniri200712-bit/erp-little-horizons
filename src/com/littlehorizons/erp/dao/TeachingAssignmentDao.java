package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.TeachingAssignment;

import java.util.List;
import java.util.Optional;

/** Teacher-subject assignments per year, section and shift (asignaciones_profesor). */
public interface TeachingAssignmentDao {

    int insert(TeachingAssignment a);

    Optional<TeachingAssignment> findById(int id);

    List<TeachingAssignment> findByTeacher(int teacherId, Integer year);

    List<TeachingAssignment> findByYear(int year);

    List<TeachingAssignment> findBySubject(int subjectId, int year);

    boolean isAssigned(int teacherId, int subjectId, int year);

    boolean delete(int id);
}
