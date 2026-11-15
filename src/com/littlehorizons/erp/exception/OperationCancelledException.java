package com.littlehorizons.erp.exception;

/** Thrown when the user cancels an operation (typing 0 in a selection prompt). */
public class OperationCancelledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OperationCancelledException() {
        super("Operación cancelada por el usuario.");
    }
}
