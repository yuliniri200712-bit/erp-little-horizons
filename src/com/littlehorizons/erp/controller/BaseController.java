package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.exception.MainMenuRequestedException;
import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.ConsoleView;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Template for module controllers: renders a submenu, runs the chosen use case and handles
 * errors in a single place (business messages are shown, technical errors are also logged).
 */
public abstract class BaseController {

    protected final ConsoleView view;

    protected BaseController(ConsoleView view) {
        this.view = view;
    }

    /** Entry point of the module (called from the main menu). */
    public abstract void start();

    /** A menu entry; {@code pauseAfter} is false for entries that open another submenu. */
    protected record Option(String label, Runnable action, boolean pauseAfter) {
        static Option action(String label, Runnable action) {
            return new Option(label, action, true);
        }

        static Option submenu(String label, Runnable action) {
            return new Option(label, action, false);
        }
    }

    protected void runMenu(String title, List<Option> options) {
        while (true) {
            int choice = view.menu(title, options.stream().map(Option::label).toList(), "Volver");
            if (choice == 0) {
                return;
            }
            Option option = options.get(choice - 1);
            execute(option.label(), option.action());
            if (option.pauseAfter()) {
                view.pause();
            }
        }
    }

    protected void execute(String context, Runnable action) {
        try {
            action.run();
        } catch (OperationCancelledException e) {
            view.warning(e.getMessage());
        } catch (BusinessException e) {
            view.error(e.getMessage());
        } catch (DataAccessException e) {
            ErrorLogger.error(getClass().getSimpleName() + " - " + context, e);
            view.error(e.getUserMessage());
        } catch (NoSuchElementException | MainMenuRequestedException e) {
            throw e; // end of input (handled by Main) or "inicio" (handled by MainController)
        } catch (RuntimeException e) {
            ErrorLogger.error(getClass().getSimpleName() + " - " + context, e);
            view.error("Error inesperado (" + e.getClass().getSimpleName() + "). El detalle quedó en error_log.txt.");
        }
    }

    protected int currentYear() {
        return LocalDate.now().getYear();
    }

    protected int readYear() {
        int year = currentYear();
        return view.readInt("Año académico", year - 10, year + 1, year);
    }
}
