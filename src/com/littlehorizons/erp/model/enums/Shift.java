package com.littlehorizons.erp.model.enums;

/** School shift of a group. */
public enum Shift implements DbEnum {
    MORNING("Mañana"),
    AFTERNOON("Tarde");

    private final String label;

    Shift(String label) {
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