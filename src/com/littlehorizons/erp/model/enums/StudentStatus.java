package com.littlehorizons.erp.model.enums;

/** Student status (Activo, Graduado, Retirado, Suspendido). */
public enum StudentStatus implements DbEnum {
    ACTIVE("Activo"),
    GRADUATED("Graduado"),
    WITHDRAWN("Retirado"),
    SUSPENDED("Suspendido");

    private final String label;

    StudentStatus(String label) {
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