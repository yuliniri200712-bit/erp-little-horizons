package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.StudentStatus;

import java.util.List;
import java.util.Optional;

/** CRUD and queries for students (estudiantes). */
public interface StudentDao extends CrudDao<Student> {

    Optional<Student> findById(int id);

    Optional<Student> findByDocument(String documentNumber);

    List<Student> findAll();

    List<Student> findByStatus(StudentStatus status);

    List<Student> search(String text);

    /** Students with an active enrollment in the given grade and year. */
    List<Student> findEnrolled(int gradeId, int year);

    int insert(Student s);

    boolean update(Student s);

    boolean updateStatus(int studentId, StudentStatus status);

    boolean delete(int id);

    long countPayments(int studentId);
}
