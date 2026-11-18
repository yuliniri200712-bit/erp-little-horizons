package com.littlehorizons.erp;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.EntityPicker;
import com.littlehorizons.erp.controller.ExtracurricularController;
import com.littlehorizons.erp.controller.FinanceController;
import com.littlehorizons.erp.controller.MainController;
import com.littlehorizons.erp.controller.ReportController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.controller.TeacherController;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.ConsoleView;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Entry point: verifies the MySQL connection, builds services ({@link AppServices}) and
 * controllers, and starts the main menu.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        ConsoleView view = new ConsoleView();
        AppConfig config = AppConfig.getInstance();
        if (!checkDatabase(view, config)) {
            System.exit(1);
            return;
        }
        try {
            buildApplication(view, AppServices.create(config)).start();
        } catch (NoSuchElementException e) {
            view.info("\nFin de la entrada estándar. Saliendo del sistema.");
        } catch (RuntimeException e) {
            ErrorLogger.error("Main", e);
            view.error("Error fatal: " + e.getMessage() + ". Consulte error_log.txt.");
            System.exit(2);
        }
    }

    private static boolean checkDatabase(ConsoleView view, AppConfig config) {
        if (!config.hasDbCredentials()) {
            view.error("No hay credenciales de base de datos. Copie config/db.properties.example a "
                    + config.dbConfigFile() + " o defina LH_DB_USER / LH_DB_PASSWORD.");
            return false;
        }
        try {
            String version = DatabaseConnection.getInstance().testConnection();
            view.info("Conectado a MySQL " + version + " (" + config.dbUrl().replaceAll("\\?.*", "") + ")");
            if (!version.matches("^([89]|\\d{2,})\\..*")) {
                view.warning("Se requiere MySQL 8.0 o superior; versión detectada: " + version);
            }
            return true;
        } catch (DataAccessException e) {
            ErrorLogger.error("Database connection at startup", e);
            view.error(e.getUserMessage());
            view.info("Sugerencia: ejecute 'docker compose up -d' y verifique config/db.properties.");
            return false;
        }
    }

    private static MainController buildApplication(ConsoleView view, AppServices services) {
        int updated = services.resources().refreshOverdue();
        if (updated > 0) {
            view.warning(updated + " préstamo(s) pasaron a estado Vencido.");
        }
        EntityPicker picker = new EntityPicker(view, services.students(), services.teachers(), services.academic(),
                services.extracurricular(), services.finance(), services.resources());
        AcademicController academic = new AcademicController(view, services.academic(), services.attendance(),
                services.students(), picker);
        ExtracurricularController extracurricular = new ExtracurricularController(view, services.extracurricular(),
                services.reports(), picker);
        FinanceController finance = new FinanceController(view, services.finance(), services.students(), picker);
        ResourceController resources = new ResourceController(view, services.resources(), picker);
        return new MainController(view, List.of(
                new StudentController(view, services.students(), picker),
                academic,
                new TeacherController(view, services.teachers(), picker),
                extracurricular,
                finance,
                resources,
                new ReportController(view, services.reports(), services.exports(), academic, extracurricular, finance,
                        resources)));
    }
}
