package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.GradeLevel;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.AttendanceStatus;
import com.littlehorizons.erp.model.report.AbsenceAlert;
import com.littlehorizons.erp.model.report.AcademicHistoryYear;
import com.littlehorizons.erp.model.report.CourseAverage;
import com.littlehorizons.erp.model.report.PeriodAverage;
import com.littlehorizons.erp.model.report.RecurrentAbsence;
import com.littlehorizons.erp.model.report.StudentRanking;
import com.littlehorizons.erp.model.report.SubjectAverage;
import com.littlehorizons.erp.util.Formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import static com.littlehorizons.erp.view.ConsoleView.row;

/** Forms and listings of subjects, subject enrollment, grades, averages and attendance. */
public class AcademicView {

    private final ConsoleView console;

    public AcademicView(ConsoleView console) {
        this.console = console;
    }

    public Subject readSubject(Subject current, List<GradeLevel> grades) {
        boolean isNew = current == null;
        console.section(isNew ? "Nueva materia" : "Actualizar materia (Enter conserva el valor)");
        String code = console.readText("Código (AREA-NN, ej: MAT-05)", isNew ? null : current.code());
        String name = console.readText("Nombre", isNew ? null : current.name());
        String description = console.readOptional("Descripción", isNew ? null : current.description());
        GradeLevel grade = isNew ? console.chooseFrom("Grado", grades, GradeLevel::name)
                : grades.stream().filter(g -> g.id() == current.gradeId()).findFirst().orElseThrow();
        if (!isNew && console.confirm("¿Cambiar el grado (actual: " + current.gradeName() + ")?")) {
            grade = console.chooseFrom("Grado", grades, GradeLevel::name);
        }
        int hours = console.readInt("Horas semanales", 1, 40, isNew ? 2 : current.weeklyHours());
        boolean active = console.readBoolean("¿Activa?", isNew || current.active());
        return new Subject(isNew ? null : current.id(), code, name, description, grade.id(), grade.name(), hours, active);
    }

    public void showSubjects(List<Subject> subjects) {
        console.table(List.of("ID", "Código", "Materia", "Grado", "Horas/sem", "Activa"),
                subjects.stream().map(s -> row(s.id(), s.code(), s.name(), s.gradeName(), s.weeklyHours(), Formatter.yesNo(s.active()))).toList());
    }

    public void showSubjectEnrollments(List<SubjectEnrollment> enrollments, boolean withStudent) {
        List<String> headers = new ArrayList<>(List.of("ID"));
        if (withStudent) {
            headers.add("Estudiante");
        }
        headers.addAll(List.of("Materia", "Año", "Período", "Sección", "Fecha", "Estado"));
        console.table(headers, enrollments.stream().map(e -> {
            List<String> cells = new ArrayList<>(row(e.id()));
            if (withStudent) {
                cells.add(e.studentName());
            }
            cells.addAll(row(e.subjectName(), e.academicYear(), e.period(), e.section(), Formatter.date(e.enrollmentDate()), e.status()));
            return cells;
        }).toList());
    }

    public void showGrades(List<AcademicRecord> records) {
        console.table(List.of("ID", "Año", "Período", "Materia", "Nota", "Profesor", "Fecha", "Observaciones"),
                records.stream().map(r -> row(r.id(), r.academicYear(), r.period(), r.subjectName(), Formatter.score(r.score()),
                        r.teacherName(), Formatter.date(r.recordDate()), r.notes())).toList());
    }

    public void showSubjectAverages(List<SubjectAverage> averages) {
        console.table(List.of("Materia", "Promedio I-IV", "Períodos", "Nota Final", "Desempeño"),
                averages.stream().map(a -> row(a.subjectName(), Formatter.average(a.average()), a.periods(),
                        Formatter.score(a.finalScore()), performance(a.average()))).toList());
    }

    public void showPeriodAverages(List<PeriodAverage> averages, OptionalDouble general) {
        console.table(List.of("Período", "Promedio general", "Materias", "Desempeño"),
                averages.stream().map(a -> row(a.period(), Formatter.average(a.average()), a.subjects(), performance(a.average()))).toList());
        console.info("Promedio general del año: " + (general.isPresent() ? Formatter.average(general.getAsDouble()) : "-"));
    }

    public void showCourseAverages(List<CourseAverage> averages) {
        console.table(List.of("Código", "Materia", "Grado", "Estudiantes", "Promedio curso", "Mínima", "Máxima"),
                averages.stream().map(a -> row(a.subjectCode(), a.subjectName(), a.gradeName(), a.students(),
                        Formatter.average(a.average()), Formatter.score(a.min()), Formatter.score(a.max()))).toList());
    }

    public void showRanking(List<StudentRanking> ranking) {
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i < ranking.size(); i++) {
            StudentRanking r = ranking.get(i);
            rows.add(row(i + 1, r.studentId(), r.studentName(), r.gradeName(), Formatter.average(r.average()), performance(r.average())));
        }
        console.table(List.of("#", "ID", "Estudiante", "Grado", "Promedio", "Desempeño"), rows);
    }

    public void showHistory(Student student, List<AcademicHistoryYear> history) {
        console.section("Historial académico de " + student.fullName() + " (" + student.documentNumber() + ")");
        if (history.isEmpty()) {
            console.info("(sin registros académicos)");
        }
        for (AcademicHistoryYear year : history) {
            console.info("");
            console.info("Año " + year.year() + " - Grado: " + year.gradeName() + " - Matrícula: " + year.enrollmentStatus()
                    + " - Promedio general: " + Formatter.average(year.generalAverage()));
            console.table(List.of("Materia", "I", "II", "III", "IV", "Final", "Promedio"),
                    year.rows().stream().map(r -> {
                        Map<AcademicPeriod, java.math.BigDecimal> s = r.scores();
                        return row(r.subjectName(), Formatter.score(s.get(AcademicPeriod.I)), Formatter.score(s.get(AcademicPeriod.II)),
                                Formatter.score(s.get(AcademicPeriod.III)), Formatter.score(s.get(AcademicPeriod.IV)),
                                Formatter.score(s.get(AcademicPeriod.FINAL)), Formatter.average(r.average()));
                    }).toList());
        }
    }

    public void showAttendance(List<Attendance> records, Map<AttendanceStatus, Long> summary) {
        console.table(List.of("Fecha", "Día", "Materia", "Estado", "Observaciones"),
                records.stream().map(a -> row(Formatter.date(a.date()), dayName(a.date()), a.subjectName(), a.status(), a.notes())).toList());
        StringBuilder text = new StringBuilder("Resumen: ");
        for (AttendanceStatus status : AttendanceStatus.values()) {
            text.append(status).append('=').append(summary.getOrDefault(status, 0L)).append("  ");
        }
        console.info(text.toString().trim());
    }

    public void showClassAttendance(List<Attendance> records) {
        console.table(List.of("Estudiante", "Materia", "Estado", "Observaciones"),
                records.stream().map(a -> row(a.studentName(), a.subjectName(), a.status(), a.notes())).toList());
    }

    public void showAbsenceAlerts(List<AbsenceAlert> alerts, int threshold) {
        console.info("Criterio: más de " + threshold + " días con ausencia en una misma semana (lunes a domingo).");
        console.table(List.of("ID", "Estudiante", "Grado", "Semana (lunes)", "Días ausente", "Registros ausente"),
                alerts.stream().map(a -> row(a.studentId(), a.studentName(), a.gradeName(), Formatter.date(a.weekStart()),
                        a.absentDays(), a.absenceRecords())).toList());
    }

    public void showRecurrentAbsences(List<RecurrentAbsence> rows) {
        console.table(List.of("ID", "Estudiante", "Grado", "Materia", "Ausencias", "Tardanzas"),
                rows.stream().map(r -> row(r.studentId(), r.studentName(), r.gradeName(), r.subjectName(), r.absences(), r.lates())).toList());
    }

    private static String performance(double average) {
        if (Double.isNaN(average)) {
            return "-";
        }
        if (average >= 4.6) {
            return "Superior";
        }
        if (average >= 4.0) {
            return "Alto";
        }
        if (average >= 3.0) {
            return "Básico";
        }
        return "Bajo";
    }

    private static String dayName(java.time.LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "Lunes";
            case TUESDAY -> "Martes";
            case WEDNESDAY -> "Miércoles";
            case THURSDAY -> "Jueves";
            case FRIDAY -> "Viernes";
            case SATURDAY -> "Sábado";
            case SUNDAY -> "Domingo";
        };
    }
}
