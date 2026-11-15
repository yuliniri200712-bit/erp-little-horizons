package com.littlehorizons.erp.model;

/** Academic grade (Preescolar = level 0 ... Once = level 11). Table: grados. */
public record GradeLevel(
        int id,
        String name,
        int level) {
}
