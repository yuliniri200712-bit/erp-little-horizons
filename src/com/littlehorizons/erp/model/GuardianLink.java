package com.littlehorizons.erp.model;

/** Guardian linked to a student. Table: estudiante_acudiente. */
public record GuardianLink(
        Guardian guardian,
        boolean primary,
        boolean emergencyContact) {
}
