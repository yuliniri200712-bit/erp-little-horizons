package com.littlehorizons.erp.model.enums;

/** Inventory resource status. */
public enum ResourceStatus implements DbEnum {
    AVAILABLE("Disponible"),
    LENT("Prestado"),
    MAINTENANCE("En Mantenimiento"),
    DECOMMISSIONED("Dado de Baja");

    private final String label;

    ResourceStatus(String label) {
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