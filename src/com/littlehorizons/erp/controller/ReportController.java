package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.service.ReportExportService;
import com.littlehorizons.erp.service.ReportExportService.ExportedFile;
import com.littlehorizons.erp.service.ReportService;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.ReportView;

import java.util.List;

import static com.littlehorizons.erp.view.ConsoleView.row;

/**
 * Menu 7 - Reportes y Estadísticas. Reuses the report flows of the module controllers so each
 * report is implemented once (grades, absences, clubs/events, arrears and overdue loans), and
 * exports the main reports to CSV files.
 */
public class ReportController extends BaseController {

    private final ReportService reportService;
    private final ReportExportService exportService;
    private final ReportView reportView;
    private final AcademicController academic;
    private final ExtracurricularController extracurricular;
    private final FinanceController finance;
    private final ResourceController resources;

    public ReportController(ConsoleView view, ReportService reportService, ReportExportService exportService,
                            AcademicController academic, ExtracurricularController extracurricular, FinanceController finance,
                            ResourceController resources) {
        super(view);
        this.reportService = reportService;
        this.exportService = exportService;
        this.reportView = new ReportView(view);
        this.academic = academic;
        this.extracurricular = extracurricular;
        this.finance = finance;
        this.resources = resources;
    }

    @Override
    public void start() {
        runMenu("REPORTES Y ESTADÍSTICAS", List.of(
                Option.action("Resumen general", () -> reportView.showDashboard(reportService.dashboard())),
                Option.action("Notas: ranking de promedios por grado y período", academic::ranking),
                Option.action("Notas: promedio del curso por materia", academic::courseAverages),
                Option.action("Notas: historial académico de un estudiante", academic::history),
                Option.action("Inasistencias: más de 3 ausencias en una semana", academic::weeklyAlerts),
                Option.action("Inasistencias: recurrentes por materia", academic::recurrent),
                Option.action("Clubes: ocupación y cupos", extracurricular::occupancyReport),
                Option.action("Eventos: participación y podios", extracurricular::eventsReport),
                Option.action("Participación extracurricular por estudiante", extracurricular::participationReport),
                Option.action("Mora: deudas con más de un mes de atraso", finance::debtReport),
                Option.action("Préstamos vencidos", resources::overdueReport),
                Option.action("Exportar reportes a archivos CSV", this::exportReports)));
    }

    private void exportReports() {
        List<ExportedFile> files = exportService.exportAll();
        if (!files.isEmpty()) {
            view.info("Carpeta: " + files.getFirst().path().toAbsolutePath().getParent());
        }
        view.table(List.of("Archivo", "Filas"), files.stream()
                .map(f -> row(f.path().getFileName(), f.rows()))
                .toList());
        view.success(files.size() + " reportes exportados (CSV separado por ';', se abren en Excel).");
    }
}
