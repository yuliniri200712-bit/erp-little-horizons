package com.littlehorizons.erp.model.enums;

/** Daily attendance status. */
public enum AttendanceStatus implements DbEnum {
    PRESENT("Presente"),
    ABSENT("Ausente"),
    LATE("Tardanza"),
    MEDICAL_EXCUSE("Excusa Médica");

    private final String label;

    AttendanceStatus(String label) {
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