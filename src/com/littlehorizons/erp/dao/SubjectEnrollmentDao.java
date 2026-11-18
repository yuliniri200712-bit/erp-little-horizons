package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;

import java.util.List;
import java.util.Optional;

/** Subject enrollments per period (inscripciones_materia). */
public interface SubjectEnrollmentDao {

    int insert(SubjectEnrollment s);

    Optional<SubjectEnrollment> findById(int id);

    boolean exists(int studentId, int subjectId, int year, AcademicPeriod period);

    /** True if the student has a non-withdrawn enrollment in the subject for the year (any period). */
    boolean isEnrolledInYear(int studentId, int subjectId, int year);

    List<SubjectEnrollment> findByStudent(int studentId, int year);

    List<SubjectEnrollment> findBySubject(int subjectId, int year, AcademicPeriod period);

    /** Distinct students enrolled (not withdrawn) in a subject during a year. */
    List<SubjectEnrollment> findStudentsOfSubjectInYear(int subjectId, int year);

    boolean updateStatus(int id, EnrollmentStatus status);

    int withdrawAllActive(int studentId, int year);
}
