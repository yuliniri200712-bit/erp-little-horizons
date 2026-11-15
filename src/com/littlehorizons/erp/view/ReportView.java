package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.report.DashboardStats;
import com.littlehorizons.erp.util.Formatter;

/** Presentation of the general statistics summary. */
public class ReportView {

    private final ConsoleView console;

    public ReportView(ConsoleView console) {
        this.console = console;
    }

    public void showDashboard(DashboardStats s) {
        console.section("Resumen general del colegio");
        console.field("Estudiantes activos", s.activeStudents());
        console.field("Acudientes registrados", s.guardians());
        console.field("Profesores activos", s.activeTeachers());
        console.field("Materias activas", s.activeSubjects());
        console.field("Clubes activos", s.activeClubs());
        console.field("Eventos próximos", s.upcomingEvents());
        console.field("Recaudo del mes", Formatter.money(s.collectedThisMonth()));
        console.field("Mora > 1 mes", Formatter.money(s.overdueDebt()));
        console.field("Préstamos vencidos", s.overdueLoans());
        console.field("Recursos disponibles", s.availableResources());
    }
}
