package com.littlehorizons.erp.model.enums;

/** Status of a receivable (charge). */
public enum ChargeStatus implements DbEnum {
    PENDING("Pendiente"),
    PAID("Pagado"),
    CANCELLED("Anulado");

    private final String label;

    ChargeStatus(String label) {
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