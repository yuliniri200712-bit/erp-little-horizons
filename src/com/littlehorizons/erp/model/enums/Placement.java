package com.littlehorizons.erp.model.enums;

/** Placement obtained in a competition. */
public enum Placement implements DbEnum {
    FIRST("1er lugar"),
    SECOND("2do lugar"),
    THIRD("3er lugar"),
    PARTICIPANT("Participante");

    private final String label;

    Placement(String label) {
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

    /** First, second and third places must be unique within an event. */
    public boolean isPodium() {
        return this != PARTICIPANT;
    }
}