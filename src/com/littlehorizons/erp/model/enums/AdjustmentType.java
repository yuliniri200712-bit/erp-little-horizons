package com.littlehorizons.erp.model.enums;

/** Student fee adjustment type. */
public enum AdjustmentType implements DbEnum {
    SCHOLARSHIP("Beca"),
    DISCOUNT("Descuento");

    private final String label;

    AdjustmentType(String label) {
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