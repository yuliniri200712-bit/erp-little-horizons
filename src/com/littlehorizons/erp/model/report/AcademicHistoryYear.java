package com.littlehorizons.erp.model.report;

import com.littlehorizons.erp.model.enums.AcademicPeriod;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** One academic year of a student's history: grade, status and grades per subject and period. */
public record AcademicHistoryYear(
        int year,
        String gradeName,
        String enrollmentStatus,
        List<Row> rows,
        Double generalAverage) {

    /** Grades of one subject in the year (periods I-IV and Final) and its average. */
    public record Row(String subjectName, Map<AcademicPeriod, BigDecimal> scores, Double average) {
    }
}
