package com.littlehorizons.erp.model;

/** Additional emergency contact of a student. Table: contactos_emergencia. */
public record EmergencyContact(
        Integer id,
        int studentId,
        String fullName,
        String relationship,
        String phone,
        String alternatePhone,
        String notes) {
}
