package com.littlehorizons.erp.model.enums;

/** Student gender. */
public enum Gender implements DbEnum {
    MALE("M"),
    FEMALE("F"),
    OTHER("Otro");

    private final String label;

    Gender(String label) {
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