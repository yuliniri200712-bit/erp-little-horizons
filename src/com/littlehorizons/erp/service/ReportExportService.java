package com.littlehorizons.erp.service;

import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.report.AbsenceAlert;
import com.littlehorizons.erp.model.report.DebtRow;
import com.littlehorizons.erp.model.report.StudentRanking;
import com.littlehorizons.erp.util.CsvWriter;
import com.littlehorizons.erp.util.ErrorLogger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Exports the main reports to CSV files in the reports folder ({@code reports.output.dir},
 * default {@code reportes/}). Each file is named with the export date, e.g. mora_20261116.csv.
 */
public class ReportExportService {

    private static final DateTimeFormatter FILE_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final FinanceService financeService;
    private final ResourceService resourceService;
    private final AttendanceService attendanceService;
    private final AcademicService academicService;
    private final ExtracurricularService extracurricularService;
    private final Path outputDir;

    public ReportExportService(FinanceService financeService, ResourceService resourceService, AttendanceService attendanceService,
                               AcademicService academicService, ExtracurricularService extracurricularService, Path outputDir) {
        this.financeService = financeService;
        this.resourceService = resourceService;
        this.attendanceService = attendanceService;
        this.academicService = academicService;
        this.extracurricularService = extracurricularService;
        this.outputDir = outputDir;
    }

    /** Result of one exported file. */
    public record ExportedFile(Path path, int rows) {
    }

    /** Writes every report and returns the generated files. */
    public List<ExportedFile> exportAll() {
        LocalDate today = LocalDate.now();
        String suffix = today.format(FILE_DATE);
        List<ExportedFile> files = new ArrayList<>();
        try {
            files.add(exportDebts(outputDir.resolve("mora_" + suffix + ".csv")));
            files.add(exportOverdueLoans(outputDir.resolve("prestamos_vencidos_" + suffix + ".csv")));
            files.add(exportAbsenceAlerts(outputDir.resolve("alertas_inasistencia_" + suffix + ".csv"),
                    today.minusWeeks(4).with(DayOfWeek.MONDAY), today));
            files.add(exportRanking(outputDir.resolve("ranking_promedios_" + today.getYear() + "_" + suffix + ".csv"), today.getYear()));
            files.add(exportClubOccupancy(outputDir.resolve("ocupacion_clubes_" + suffix + ".csv")));
        } catch (IOException e) {
            ErrorLogger.error("Report export to " + outputDir.toAbsolutePath(), e);
            throw new BusinessException("No se pudieron escribir los reportes en " + outputDir.toAbsolutePath()
                    + ": " + e.getMessage());
        }
        return files;
    }

    private ExportedFile exportDebts(Path file) throws IOException {
        List<DebtRow> debts = financeService.debtsOverOneMonth();
        int rows = CsvWriter.write(file, List.of("Cobro", "Estudiante", "Grado", "Acudiente", "Teléfono", "Concepto",
                        "Descripción", "Vencimiento", "Días de mora", "Valor", "Abonado", "Saldo"), debts,
                d -> List.of(d.chargeId(), d.studentName(), d.gradeName(), nullSafe(d.guardianName()), nullSafe(d.guardianPhone()),
                        d.conceptName(), d.description(), d.dueDate(), d.daysOverdue(), plain(d.amount()), plain(d.paid()),
                        plain(d.pending())));
        return new ExportedFile(file, rows);
    }

    private ExportedFile exportOverdueLoans(Path file) throws IOException {
        List<Loan> loans = resourceService.overdueLoans();
        int rows = CsvWriter.write(file, List.of("Préstamo", "Tipo", "Solicitante", "Código recurso", "Recurso",
                        "Fecha préstamo", "Devolución prevista", "Días vencido"), loans,
                l -> List.of(l.id(), l.borrowerType().label(), l.borrowerName(), l.resourceCode(), l.resourceName(),
                        l.loanDate(), l.expectedReturnDate(), l.daysOverdue()));
        return new ExportedFile(file, rows);
    }

    private ExportedFile exportAbsenceAlerts(Path file, LocalDate from, LocalDate to) throws IOException {
        List<AbsenceAlert> alerts = attendanceService.weeklyAbsenceAlerts(from, to);
        int rows = CsvWriter.write(file, List.of("ID estudiante", "Estudiante", "Grado", "Semana (lunes)",
                        "Días ausente", "Registros ausente"), alerts,
                a -> List.of(a.studentId(), a.studentName(), a.gradeName(), a.weekStart(), a.absentDays(), a.absenceRecords()));
        return new ExportedFile(file, rows);
    }

    private ExportedFile exportRanking(Path file, int year) throws IOException {
        List<StudentRanking> ranking = academicService.ranking(null, year, null);
        int rows = CsvWriter.write(file, List.of("ID estudiante", "Estudiante", "Grado", "Promedio"), ranking,
                r -> List.of(r.studentId(), r.studentName(), r.gradeName(), String.format(Locale.US, "%.2f", r.average())));
        return new ExportedFile(file, rows);
    }

    private ExportedFile exportClubOccupancy(Path file) throws IOException {
        List<Club> clubs = extracurricularService.clubsByOccupancy();
        int rows = CsvWriter.write(file, List.of("Club", "Encargado", "Cupo", "Miembros activos", "Activo"), clubs,
                c -> List.of(c.name(), nullSafe(c.supervisorName()), c.maxCapacity(), c.activeMembers(), c.active() ? "Sí" : "No"));
        return new ExportedFile(file, rows);
    }

    private static String plain(java.math.BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
