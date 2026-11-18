package com.littlehorizons.erp.exception;

/**
 * Thrown when the user types the home keyword ("inicio") at any prompt. Unlike
 * {@link OperationCancelledException}, no module menu handles it: it travels up through every
 * submenu until {@code MainController} catches it and shows the main menu again.
 */
public class MainMenuRequestedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MainMenuRequestedException() {
        super("Regresando al menú principal.");
    }
}
