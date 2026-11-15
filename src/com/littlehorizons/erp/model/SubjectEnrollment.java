package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;
import java.time.LocalDate;

/** Enrollment of a student in a subject for a period. Table: inscripciones_materia. */
public record SubjectEnrollment(
        Integer id,
        int studentId,
        String studentName,
        int subjectId,
        String subjectName,
        int academicYear,
        AcademicPeriod period,
        String section,
        LocalDate enrollmentDate,
        EnrollmentStatus status) {
}
