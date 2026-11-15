package com.littlehorizons.erp.model.report;

/** Recurrent absences of a student in a subject within a date range. */
public record RecurrentAbsence(
        int studentId,
        String studentName,
        String gradeName,
        String subjectName,
        int absences,
        int lates) {
}
