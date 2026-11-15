package com.littlehorizons.erp.model.report;

/** Extracurricular participation of a student. */
public record StudentParticipation(
        int studentId,
        String studentName,
        String gradeName,
        long activeClubs,
        long events,
        long podiums) {
}
