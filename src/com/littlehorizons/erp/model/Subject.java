package com.littlehorizons.erp.model;

/** Subject of a grade. Table: materias. */
public record Subject(
        Integer id,
        String code,
        String name,
        String description,
        int gradeId,
        String gradeName,
        int weeklyHours,
        boolean active) {
}
