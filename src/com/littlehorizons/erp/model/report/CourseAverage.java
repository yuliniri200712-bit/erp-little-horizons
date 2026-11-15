package com.littlehorizons.erp.model.report;

import java.math.BigDecimal;

/** Course average of a subject. */
public record CourseAverage(
        String subjectCode,
        String subjectName,
        String gradeName,
        long students,
        double average,
        BigDecimal min,
        BigDecimal max) {
}
