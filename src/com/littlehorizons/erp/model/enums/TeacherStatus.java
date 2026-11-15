package com.littlehorizons.erp.model.enums;

/** Teacher employment status. */
public enum TeacherStatus implements DbEnum {
    ACTIVE("Activo"),
    INACTIVE("Inactivo"),
    ON_LEAVE("Licencia");

    private final String label;

    TeacherStatus(String label) {
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