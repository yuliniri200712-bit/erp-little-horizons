package com.littlehorizons.erp.model.enums;

/** Identity document types. */
public enum DocumentType implements DbEnum {
    TI("TI"),
    CC("CC"),
    CE("CE"),
    PASSPORT("Pasaporte"),
    RC("RC");

    private final String label;

    DocumentType(String label) {
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

    /** Document types accepted for adults (guardians and teachers). */
    public static DocumentType[] adultTypes() {
        return new DocumentType[]{CC, CE, PASSPORT};
    }
}