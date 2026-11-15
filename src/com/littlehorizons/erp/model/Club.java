package com.littlehorizons.erp.model;

/** Extracurricular club. Table: clubes. */
public record Club(
        Integer id,
        String name,
        String description,
        Integer supervisorId,
        String supervisorName,
        int maxCapacity,
        String meetingSchedule,
        boolean active,
        int activeMembers) {
}
