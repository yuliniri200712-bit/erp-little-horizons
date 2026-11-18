package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** Yearly enrollments (matriculas). */
public interface EnrollmentDao {

    int insert(Enrollment e);

    Optional<Enrollment> findById(int id);

    List<Enrollment> findByStudent(int studentId);

    Optional<Enrollment> findByStudentAndYear(int studentId, int year);

    Optional<Enrollment> findLatest(int studentId);

    List<Enrollment> findByYear(int year, Integer gradeId);

    boolean updateStatus(int enrollmentId, EnrollmentStatus status, LocalDate withdrawalDate, String notes);

    boolean delete(int id);
}
