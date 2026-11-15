package com.littlehorizons.erp.model.enums;

/** Loan status. */
public enum LoanStatus implements DbEnum {
    ACTIVE("Activo"),
    RETURNED("Devuelto"),
    OVERDUE("Vencido");

    private final String label;

    LoanStatus(String label) {
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