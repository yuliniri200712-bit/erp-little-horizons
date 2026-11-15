package com.littlehorizons.erp.model.enums;

/** Status of a yearly enrollment or a subject enrollment. */
public enum EnrollmentStatus implements DbEnum {
    ACTIVE("Activa"),
    COMPLETED("Completada"),
    WITHDRAWN("Retirada");

    private final String label;

    EnrollmentStatus(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}