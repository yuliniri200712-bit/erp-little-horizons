package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.exception.MainMenuRequestedException;
import com.littlehorizons.erp.view.ConsoleView;

import java.util.List;

/** Main menu of the ERP; delegates each module to its controller. */
public class MainController {

    private static final String TITLE = "ERP Colegio Little Horizons";
    private static final String SUBTITLE = "Sistema de Gestión Educativa";
    private static final List<String> OPTIONS = List.of(
            "Gestión de Estudiantes",
            "Gestión Académica",
            "Gestión de Profesores",
            "Actividades Extracurriculares",
            "Gestión Financiera",
            "Gestión de Recursos",
            "Reportes y Estadísticas",
            "Salir");

    private final ConsoleView view;
    private final List<BaseController> modules;

    /** {@code modules} must follow the order of the menu options 1-7. */
    public MainController(ConsoleView view, List<BaseController> modules) {
        if (modules.size() != OPTIONS.size() - 1) {
            throw new IllegalArgumentException("Se esperaban " + (OPTIONS.size() - 1) + " módulos");
        }
        this.view = view;
        this.modules = List.copyOf(modules);
    }

    public void start() {
        while (true) {
            view.banner(TITLE, SUBTITLE);
            for (int i = 0; i < OPTIONS.size(); i++) {
                view.info((i + 1) + ". " + OPTIONS.get(i));
            }
            view.info("");
            int choice;
            try {
                choice = view.readInt("Seleccione una opción", 1, OPTIONS.size());
            } catch (MainMenuRequestedException alreadyHome) {
                continue;
            }
            if (choice == OPTIONS.size()) {
                view.info("Gracias por usar el ERP Little Horizons. ¡Hasta pronto!");
                return;
            }
            try {
                modules.get(choice - 1).start();
            } catch (MainMenuRequestedException home) {
                view.info(home.getMessage());
            }
        }
    }
}
