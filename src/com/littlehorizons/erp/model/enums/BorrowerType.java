package com.littlehorizons.erp.model.enums;

/**
 * Who borrows a resource. Each type maps to its own loan table
 * (prestamos_estudiante / prestamos_profesor), as required by the specification.
 */
public enum BorrowerType implements DbEnum {
    STUDENT("Estudiante", "prestamos_estudiante", "id_estudiante", "estudiantes", "id_estudiante"),
    TEACHER("Profesor", "prestamos_profesor", "id_profesor", "profesores", "id_profesor");

    private final String label;
    private final String loanTable;
    private final String borrowerColumn;
    private final String personTable;
    private final String personKey;

    BorrowerType(String label, String loanTable, String borrowerColumn, String personTable, String personKey) {
        this.label = label;
        this.loanTable = loanTable;
        this.borrowerColumn = borrowerColumn;
        this.personTable = personTable;
        this.personKey = personKey;
    }

    @Override
    public String label() {
        return label;
    }

    public String loanTable() {
        return loanTable;
    }

    public String borrowerColumn() {
        return borrowerColumn;
    }

    public String personTable() {
        return personTable;
    }

    public String personKey() {
        return personKey;
    }

    @Override
    public String toString() {
        return label;
    }
}
