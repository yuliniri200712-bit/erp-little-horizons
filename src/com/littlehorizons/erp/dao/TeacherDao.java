package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Teacher;

import java.util.List;
import java.util.Optional;

/** CRUD for teachers (profesores). */
public interface TeacherDao extends CrudDao<Teacher> {

    Optional<Teacher> findById(int id);

    Optional<Teacher> findByDocument(String documentNumber);

    List<Teacher> findAll();

    List<Teacher> search(String text);

    int insert(Teacher t);

    boolean update(Teacher t);

    boolean delete(int id);

    /** Records that reference the teacher without cascade (grades, attendance, clubs, events). */
    long countBlockingReferences(int teacherId);
}
