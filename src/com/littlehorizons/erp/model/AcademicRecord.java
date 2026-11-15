package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.AcademicPeriod;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Grade (0.0-5.0) of a student in a subject and period. Table: registros_academicos. */
public record AcademicRecord(
        Integer id,
        int studentId,
        int subjectId,
        String subjectName,
        int teacherId,
        String teacherName,
        int academicYear,
        AcademicPeriod period,
        BigDecimal score,
        String notes,
        LocalDate recordDate) {
}
