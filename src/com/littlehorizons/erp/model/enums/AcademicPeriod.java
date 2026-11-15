package com.littlehorizons.erp.model.enums;

/** Academic periods; FINAL is only valid for grades. */
public enum AcademicPeriod implements DbEnum {
    I("I"),
    II("II"),
    III("III"),
    IV("IV"),
    FINAL("Final");

    private final String label;

    AcademicPeriod(String label) {
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

    /** Periods I-IV are used for subject enrollment; FINAL only for grades. */
    public boolean isRegular() {
        return this != FINAL;
    }

    public static AcademicPeriod[] regularPeriods() {
        return new AcademicPeriod[]{I, II, III, IV};
    }
}