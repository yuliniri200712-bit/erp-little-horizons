package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.EnrollmentStatus;
import java.time.LocalDate;

/** Yearly enrollment (matricula) of a student in a grade. Table: matriculas. */
public record Enrollment(
        Integer id,
        int studentId,
        int gradeId,
        String gradeName,
        int gradeLevel,
        int academicYear,
        EnrollmentStatus status,
        LocalDate enrollmentDate,
        LocalDate withdrawalDate,
        String notes) {
}
