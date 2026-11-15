package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.GradeLevel;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.AttendanceStatus;
import com.littlehorizons.erp.service.AcademicService;
import com.littlehorizons.erp.service.AttendanceService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.util.Formatter;
import com.littlehorizons.erp.view.AcademicView;
import com.littlehorizons.erp.view.ConsoleView;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Menu 2 - Gestión Académica: subjects, subject enrollment, grades, averages and attendance. */
public class AcademicController extends BaseController {

    private final AcademicService academicService;
    private final AttendanceService attendanceService;
    private final StudentService studentService;
    private final AcademicView academicView;
    private final EntityPicker picker;

    public AcademicController(ConsoleView view, AcademicService academicService, AttendanceService attendanceService,
                              StudentService studentService, EntityPicker picker) {
        super(view);
        this.academicService = academicService;
        this.attendanceService = attendanceService;
        this.studentService = studentService;
        this.academicView = new AcademicView(view);
        this.picker = picker;
    }

    @Override
    public void start() {
        runMenu("GESTIÓN ACADÉMICA", List.of(
                Option.submenu("Materias", this::subjectsMenu),
                Option.submenu("Inscripción de materias por período", this::enrollmentMenu),
                Option.submenu("Calificaciones y promedios", this::gradesMenu),
                Option.submenu("Asistencia", this::attendanceMenu)));
    }

    // ---------------------------------------------------------------- subjects

    private void subjectsMenu() {
        runMenu("MATERIAS", List.of(
                Option.action("Listar todas las materias", () -> academicView.showSubjects(academicService.listSubjects())),
                Option.action("Listar materias de un grado", () -> academicView.showSubjects(academicService.subjectsOfGrade(picker.grade().id()))),
                Option.action("Registrar materia", () -> {
                    int id = academicService.createSubject(academicView.readSubject(null, studentService.grades()));
                    view.success("Materia registrada con ID " + id + ".");
                }),
                Option.action("Actualizar materia", () -> {
                    Subject current = picker.subject();
                    academicService.updateSubject(academicView.readSubject(current, studentService.grades()));
                    view.success("Materia actualizada.");
                }),
                Option.action("Eliminar materia", () -> {
                    Subject subject = picker.subject();
                    if (view.confirm("¿Confirma eliminar " + subject.code() + " " + subject.name() + "?")) {
                        academicService.deleteSubject(subject.id());
                        view.success("Materia eliminada.");
                    }
                })));
    }

    // ---------------------------------------------------------------- subject enrollment

    private void enrollmentMenu() {
        runMenu("INSCRIPCIÓN DE MATERIAS", List.of(
                Option.action("Inscribir estudiante en una materia", () -> {
                    Student student = picker.student();
                    Subject subject = picker.subject();
                    int year = readYear();
                    AcademicPeriod period = view.chooseEnum("Período", AcademicPeriod.regularPeriods(), null);
                    String section = view.readText("Sección", "A");
                    int id = academicService.enrollInSubject(student.id(), subject.id(), year, period, section);
                    view.success("Inscripción #" + id + " registrada.");
                }),
                Option.action("Inscribir estudiante en TODAS las materias de su grado", () -> {
                    Student student = picker.student();
                    int year = readYear();
                    AcademicPeriod period = view.chooseEnum("Período", AcademicPeriod.regularPeriods(), null);
                    String section = view.readText("Sección", "A");
                    int created = academicService.enrollInAllSubjects(student.id(), year, period, section);
                    view.success(created + " inscripción(es) registrada(s) para el período " + period + ".");
                }),
                Option.action("Ver materias inscritas de un estudiante", () -> {
                    Student student = picker.student();
                    academicView.showSubjectEnrollments(academicService.subjectEnrollmentsOf(student.id(), readYear()), false);
                }),
                Option.action("Ver estudiantes inscritos en una materia", () -> {
                    Subject subject = picker.subject();
                    int year = readYear();
                    AcademicPeriod period = view.chooseEnum("Período", AcademicPeriod.regularPeriods(), null);
                    academicView.showSubjectEnrollments(academicService.studentsInSubject(subject.id(), year, period), true);
                }),
                Option.action("Retirar una inscripción", () -> {
                    Student student = picker.student();
                    List<SubjectEnrollment> list = academicService.subjectEnrollmentsOf(student.id(), readYear());
                    academicView.showSubjectEnrollments(list, false);
                    int id = view.readInt("ID de la inscripción", 1, Integer.MAX_VALUE);
                    academicService.withdrawSubjectEnrollment(id);
                    view.success("Inscripción retirada.");
                })));
    }

    // ---------------------------------------------------------------- grades

    private void gradesMenu() {
        runMenu("CALIFICACIONES Y PROMEDIOS", List.of(
                Option.action("Registrar / actualizar nota", this::recordGrade),
                Option.action("Ver notas de un estudiante por año", () -> {
                    Student student = picker.student();
                    academicView.showGrades(academicService.gradesOf(student.id(), readYear()));
                }),
                Option.action("Promedio por materia de un estudiante", () -> {
                    Student student = picker.student();
                    academicView.showSubjectAverages(academicService.subjectAverages(student.id(), readYear()));
                }),
                Option.action("Promedio general de un estudiante por período", () -> {
                    Student student = picker.student();
                    int year = readYear();
                    academicView.showPeriodAverages(academicService.periodAverages(student.id(), year),
                            academicService.generalAverage(student.id(), year));
                }),
                Option.action("Promedio del curso por materia", this::courseAverages),
                Option.action("Historial académico completo", this::history),
                Option.action("Ranking de promedios", this::ranking),
                Option.action("Eliminar una nota", () -> {
                    Student student = picker.student();
                    academicView.showGrades(academicService.gradesOf(student.id(), readYear()));
                    int id = view.readInt("ID de la nota", 1, Integer.MAX_VALUE);
                    if (view.confirm("¿Confirma eliminar la nota " + id + "?")) {
                        academicService.deleteGrade(id);
                        view.success("Nota eliminada.");
                    }
                })));
    }

    void courseAverages() {
        GradeLevel grade = picker.grade();
        int year = readYear();
        AcademicPeriod period = view.chooseOptionalEnum("Período", AcademicPeriod.regularPeriods());
        academicView.showCourseAverages(academicService.courseAverages(grade.id(), year, period));
    }

    void history() {
        Student student = picker.student();
        academicView.showHistory(student, academicService.academicHistory(student.id()));
    }

    /** Ranking of general averages, optionally by grade and period. */
    void ranking() {
        Integer gradeId = view.confirm("¿Filtrar por un grado?") ? picker.grade().id() : null;
        int year = readYear();
        AcademicPeriod period = view.chooseOptionalEnum("Período", AcademicPeriod.regularPeriods());
        academicView.showRanking(academicService.ranking(gradeId, year, period));
    }

    private void recordGrade() {
        Student student = picker.student();
        Subject subject = picker.subject();
        int year = readYear();
        AcademicPeriod period = view.chooseEnum("Período", AcademicPeriod.values(), null);
        Optional<AcademicRecord> existing = academicService.findGrade(student.id(), subject.id(), year, period);
        if (existing.isPresent()) {
            view.warning("Ya existe la nota " + Formatter.score(existing.get().score()) + " para ese período.");
            if (!view.confirm("¿Desea reemplazarla?")) {
                return;
            }
        }
        Teacher teacher = picker.teacher();
        BigDecimal score = view.readValidated("Nota (0.0 a 5.0, un decimal)", academicService::parseScore);
        String notes = view.readOptional("Observaciones", existing.map(AcademicRecord::notes).orElse(null));
        LocalDate date = view.readDate("Fecha de registro", LocalDate.now());
        AcademicRecord saved = academicService.saveGrade(student.id(), subject.id(), teacher.id(), year, period, score, notes, date);
        view.success("Nota " + Formatter.score(saved.score()) + " guardada para " + student.fullName() + " en " + subject.name()
                + " (" + period + " " + year + ").");
    }

    // ---------------------------------------------------------------- attendance

    private void attendanceMenu() {
        runMenu("ASISTENCIA", List.of(
                Option.action("Registrar asistencia de un estudiante", this::recordOne),
                Option.action("Registrar asistencia de una clase completa", this::recordClass),
                Option.action("Consultar asistencia de un estudiante", () -> {
                    Student student = picker.student();
                    LocalDate from = view.readDate("Desde", LocalDate.now().minusDays(30));
                    LocalDate to = view.readDate("Hasta", LocalDate.now());
                    List<Attendance> records = attendanceService.attendanceOf(student.id(), from, to);
                    academicView.showAttendance(records, attendanceService.summarize(records));
                }),
                Option.action("Consultar asistencia de una clase en una fecha", () -> {
                    Subject subject = picker.subject();
                    academicView.showClassAttendance(attendanceService.attendanceOfClass(subject.id(), view.readDate("Fecha", LocalDate.now())));
                }),
                Option.action("Reporte: más de 3 ausencias en una semana", this::weeklyAlerts),
                Option.action("Reporte: inasistencias recurrentes por materia", this::recurrent)));
    }

    private void recordOne() {
        Student student = picker.student();
        Subject subject = picker.subject();
        LocalDate date = view.readDate("Fecha", LocalDate.now());
        AttendanceStatus status = view.chooseEnum("Estado", AttendanceStatus.values(), AttendanceStatus.PRESENT);
        String notes = view.readOptional("Observaciones", null);
        Integer teacherId = picker.optionalTeacherId("Registrado por");
        attendanceService.record(student.id(), subject.id(), date, status, notes, teacherId);
        view.success("Asistencia registrada: " + student.fullName() + " - " + status + ".");
    }

    private void recordClass() {
        Subject subject = picker.subject();
        LocalDate date = view.readDate("Fecha", LocalDate.now());
        Integer teacherId = picker.optionalTeacherId("Registrado por");
        Map<Integer, String> roster = attendanceService.namesOf(attendanceService.classRoster(subject.id(), date));
        view.info("Para cada estudiante escriba: P=Presente, A=Ausente, T=Tardanza, E=Excusa Médica (Enter = Presente).");
        Map<Integer, AttendanceStatus> statuses = new LinkedHashMap<>();
        roster.forEach((studentId, name) -> statuses.put(studentId, view.readValidated("  " + name, AcademicController::parseStatus)));
        int saved = attendanceService.recordClass(subject.id(), date, teacherId, statuses);
        view.success("Asistencia guardada para " + saved + " estudiantes de " + subject.name() + " el " + Formatter.date(date) + ".");
    }

    private static AttendanceStatus parseStatus(String text) {
        return switch (text.trim().toUpperCase()) {
            case "", "P" -> AttendanceStatus.PRESENT;
            case "A" -> AttendanceStatus.ABSENT;
            case "T" -> AttendanceStatus.LATE;
            case "E" -> AttendanceStatus.MEDICAL_EXCUSE;
            default -> throw new IllegalArgumentException("Use P, A, T o E.");
        };
    }

    void weeklyAlerts() {
        LocalDate today = LocalDate.now();
        LocalDate from = view.readDate("Desde", today.minusWeeks(4).with(DayOfWeek.MONDAY));
        LocalDate to = view.readDate("Hasta", today);
        academicView.showAbsenceAlerts(attendanceService.weeklyAbsenceAlerts(from, to), AttendanceService.WEEKLY_ABSENCE_THRESHOLD);
    }

    void recurrent() {
        LocalDate from = view.readDate("Desde", LocalDate.now().minusMonths(1));
        LocalDate to = view.readDate("Hasta", LocalDate.now());
        int min = view.readInt("Mínimo de ausencias por materia", 1, 100, 3);
        academicView.showRecurrentAbsences(attendanceService.recurrentAbsences(from, to, min));
    }
}
