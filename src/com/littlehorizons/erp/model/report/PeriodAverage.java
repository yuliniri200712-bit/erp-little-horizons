package com.littlehorizons.erp.model.report;

import com.littlehorizons.erp.model.enums.AcademicPeriod;

/** General average of a student in one period. */
public record PeriodAverage(
        AcademicPeriod period,
        double average,
        long subjects) {
}
