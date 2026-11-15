package com.littlehorizons.erp.exception;

/**
 * Raised when an operation violates a business rule or a validation (e.g. club without seats,
 * grade out of range, resource not available). Its message is shown directly to the user.
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }
}
