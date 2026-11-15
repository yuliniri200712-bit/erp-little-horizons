package com.littlehorizons.erp.model.report;

import java.time.LocalDate;

/** Student absent on more than N days of the same week. */
public record AbsenceAlert(
        int studentId,
        String studentName,
        String gradeName,
        LocalDate weekStart,
        int absentDays,
        int absenceRecords) {
}
