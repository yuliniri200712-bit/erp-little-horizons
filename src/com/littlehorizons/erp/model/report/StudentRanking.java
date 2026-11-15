package com.littlehorizons.erp.model.report;

/** Student general average used for rankings. */
public record StudentRanking(
        int studentId,
        String studentName,
        String gradeName,
        double average) {
}
