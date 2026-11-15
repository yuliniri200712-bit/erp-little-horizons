package com.littlehorizons.erp.model.enums;

/** Club membership status. */
public enum MembershipStatus implements DbEnum {
    ACTIVE("Activa"),
    WITHDRAWN("Retirada");

    private final String label;

    MembershipStatus(String label) {
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