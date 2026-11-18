package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Subject;

import java.util.List;
import java.util.Optional;

/** CRUD for subjects (materias). */
public interface SubjectDao extends CrudDao<Subject> {

    Optional<Subject> findById(int id);

    Optional<Subject> findByCode(String code);

    List<Subject> findAll();

    List<Subject> findByGrade(int gradeId, boolean activeOnly);

    int insert(Subject s);

    boolean update(Subject s);

    boolean delete(int id);

    long countDependencies(int subjectId);
}
