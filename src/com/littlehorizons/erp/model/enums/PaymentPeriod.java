package com.littlehorizons.erp.model.enums;

/** Period a payment applies to. */
public enum PaymentPeriod implements DbEnum {
    I("I"),
    II("II"),
    III("III"),
    IV("IV"),
    ANNUAL("Anual");

    private final String label;

    PaymentPeriod(String label) {
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