package com.littlehorizons.erp.model.report;

import java.math.BigDecimal;

/** Average of a student in one subject (periods I-IV) plus the final grade if any. */
public record SubjectAverage(
        String subjectName,
        double average,
        long periods,
        BigDecimal finalScore) {
}
