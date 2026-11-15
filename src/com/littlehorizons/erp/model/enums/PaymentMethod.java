package com.littlehorizons.erp.model.enums;

/** Accepted payment methods. */
public enum PaymentMethod implements DbEnum {
    CASH("Efectivo"),
    TRANSFER("Transferencia"),
    CARD("Tarjeta");

    private final String label;

    PaymentMethod(String label) {
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