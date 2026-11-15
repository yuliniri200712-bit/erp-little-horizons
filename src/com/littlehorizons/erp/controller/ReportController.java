package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.service.ReportService;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.ReportView;

import java.util.List;

/**
 * Menu 7 - Reportes y Estadísticas. Reuses the report flows of the module controllers so each
 * report is implemented once (grades, absences, clubs/events, arrears and overdue loans).
 */
public class ReportController extends BaseController {

    private final ReportService reportService;
    private final ReportView reportView;
    private final AcademicController academic;
    private final ExtracurricularController extracurricular;
    private final FinanceController finance;
    private final ResourceController resources;

    public ReportController(ConsoleView view, ReportService reportService, AcademicController academic,
                            ExtracurricularController extracurricular, FinanceController finance, ResourceController resources) {
        super(view);
        this.reportService = reportService;
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
                Option.action("Préstamos vencidos", resources::overdueReport)));
    }
}
