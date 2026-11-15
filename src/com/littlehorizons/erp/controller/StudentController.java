package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.GradeLevel;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.StudentView;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/** Menu 1 - Gestión de Estudiantes: students, guardians, emergency contacts and enrollment. */
public class StudentController extends BaseController {

    private final StudentService service;
    private final StudentView studentView;
    private final EntityPicker picker;

    public StudentController(ConsoleView view, StudentService service, EntityPicker picker) {
        super(view);
        this.service = service;
        this.studentView = new StudentView(view);
        this.picker = picker;
    }

    @Override
    public void start() {
        runMenu("GESTIÓN DE ESTUDIANTES", List.of(
                Option.action("Listar estudiantes", this::listStudents),
                Option.action("Buscar estudiante por nombre o documento", this::search),
                Option.action("Ver ficha completa del estudiante", this::showDetail),
                Option.action("Registrar estudiante (con acudiente)", this::register),
                Option.action("Actualizar datos de un estudiante", this::update),
                Option.action("Eliminar estudiante", this::delete),
                Option.action("Cambiar estado (Activo / Graduado / Retirado / Suspendido)", this::changeStatus),
                Option.action("Matricular estudiante (grado y año)", this::enroll),
                Option.action("Listar matriculados por grado y año", this::listEnrolled),
                Option.action("Resumen de matrícula por año", this::enrollmentSummary),
                Option.submenu("Acudientes", this::guardiansMenu),
                Option.submenu("Contactos de emergencia", this::contactsMenu)));
    }

    // ---------------------------------------------------------------- students

    private void listStudents() {
        StudentStatus status = view.chooseOptionalEnum("Filtrar por estado", StudentStatus.values());
        studentView.showStudents(status == null ? service.listStudents() : service.listStudents(status));
    }

    private void search() {
        studentView.showStudents(service.searchStudents(view.readRequired("Nombre, apellido o documento")));
    }

    private void showDetail() {
        Student student = picker.student();
        studentView.showStudentDetail(student, service.guardiansOf(student.id()), service.emergencyContactsOf(student.id()),
                service.enrollmentsOf(student.id()));
    }

    private void register() {
        Student student = studentView.readStudent(null);
        view.section("Acudiente del estudiante (obligatorio)");
        Integer existingGuardianId = null;
        Guardian newGuardian = null;
        if (view.confirm("¿El acudiente ya está registrado en el sistema?")) {
            existingGuardianId = picker.guardian().id();
        } else {
            newGuardian = studentView.readGuardian(null);
        }
        int id = service.registerStudent(student, existingGuardianId, newGuardian);
        view.success("Estudiante registrado con ID " + id + " y acudiente vinculado.");
        if (view.confirm("¿Desea matricularlo ahora?")) {
            enrollStudent(id);
        }
    }

    private void update() {
        Student current = picker.student();
        service.updateStudent(studentView.readStudent(current));
        view.success("Datos del estudiante actualizados.");
    }

    private void delete() {
        Student student = picker.student();
        view.warning("Se eliminarán también sus matrículas, notas, asistencias, membresías y préstamos.");
        if (view.confirm("¿Confirma eliminar a " + student.fullName() + "?")) {
            service.deleteStudent(student.id());
            view.success("Estudiante eliminado.");
        }
    }

    private void changeStatus() {
        Student student = picker.student();
        view.info("Estado actual: " + student.status());
        StudentStatus status = view.chooseEnum("Nuevo estado", StudentStatus.values(), null);
        LocalDate date = view.readDate("Fecha del cambio", LocalDate.now());
        String reason = view.readOptional("Motivo / observación", null);
        service.changeStatus(student.id(), status, date, reason);
        view.success("Estado actualizado a " + status + ".");
    }

    private void enroll() {
        enrollStudent(picker.student().id());
    }

    private void enrollStudent(int studentId) {
        GradeLevel grade = picker.grade();
        int year = readYear();
        LocalDate date = view.readDate("Fecha de matrícula", LocalDate.now());
        String notes = view.readOptional("Observaciones", null);
        int id = service.enroll(studentId, grade.id(), year, date, notes);
        view.success("Matrícula #" + id + " registrada: " + grade.name() + " " + year + ".");
    }

    private void listEnrolled() {
        GradeLevel grade = picker.grade();
        int year = readYear();
        studentView.showStudents(service.enrolledStudents(grade.id(), year));
    }

    private void enrollmentSummary() {
        int year = readYear();
        List<Enrollment> enrollments = service.enrollmentsByYear(year, null);
        Map<String, Map<String, Long>> summary = enrollments.stream().collect(Collectors.groupingBy(
                e -> String.format("%02d %s", e.gradeLevel(), e.gradeName()), TreeMap::new,
                Collectors.groupingBy(e -> e.status().label(), Collectors.counting())));
        Map<String, Map<String, Long>> named = new java.util.LinkedHashMap<>();
        summary.forEach((key, value) -> named.put(key.substring(3), value));
        studentView.showEnrollmentSummary(year, named);
    }

    // ---------------------------------------------------------------- guardians

    private void guardiansMenu() {
        runMenu("ACUDIENTES", List.of(
                Option.action("Listar acudientes", () -> studentView.showGuardians(service.listGuardians())),
                Option.action("Ver acudiente y estudiantes a cargo", () -> {
                    Guardian g = picker.guardian();
                    studentView.showGuardianDetail(g, service.studentsOf(g.id()));
                }),
                Option.action("Registrar acudiente", () -> {
                    int id = service.createGuardian(studentView.readGuardian(null));
                    view.success("Acudiente registrado con ID " + id + ".");
                }),
                Option.action("Actualizar acudiente", () -> {
                    service.updateGuardian(studentView.readGuardian(picker.guardian()));
                    view.success("Acudiente actualizado.");
                }),
                Option.action("Eliminar acudiente", () -> {
                    Guardian g = picker.guardian();
                    if (view.confirm("¿Confirma eliminar a " + g.fullName() + "?")) {
                        service.deleteGuardian(g.id());
                        view.success("Acudiente eliminado.");
                    }
                }),
                Option.action("Vincular acudiente a un estudiante", () -> {
                    Student s = picker.student();
                    Guardian g = picker.guardian();
                    boolean primary = view.confirm("¿Será el acudiente principal?");
                    boolean emergency = view.confirm("¿Es contacto de emergencia?");
                    service.linkGuardian(s.id(), g.id(), primary, emergency);
                    view.success(g.fullName() + " vinculado a " + s.fullName() + ".");
                }),
                Option.action("Desvincular acudiente de un estudiante", () -> {
                    Student s = picker.student();
                    studentView.showGuardianLinks(service.guardiansOf(s.id()));
                    Guardian g = picker.guardian();
                    service.unlinkGuardian(s.id(), g.id());
                    view.success("Acudiente desvinculado.");
                }),
                Option.action("Ver acudientes de un estudiante", () ->
                        studentView.showGuardianLinks(service.guardiansOf(picker.student().id())))));
    }

    // ---------------------------------------------------------------- emergency contacts

    private void contactsMenu() {
        runMenu("CONTACTOS DE EMERGENCIA", List.of(
                Option.action("Ver contactos de un estudiante (incluye acudientes)", () -> {
                    Student s = picker.student();
                    view.section("Acudientes");
                    studentView.showGuardianLinks(service.guardiansOf(s.id()));
                    view.section("Contactos adicionales");
                    studentView.showContacts(service.emergencyContactsOf(s.id()));
                }),
                Option.action("Agregar contacto de emergencia", () -> {
                    Student s = picker.student();
                    int id = service.addEmergencyContact(studentView.readContact(s.id()));
                    view.success("Contacto #" + id + " agregado.");
                }),
                Option.action("Eliminar contacto de emergencia", () -> {
                    Student s = picker.student();
                    studentView.showContacts(service.emergencyContactsOf(s.id()));
                    int id = view.readInt("ID del contacto", 1, Integer.MAX_VALUE);
                    service.deleteEmergencyContact(s.id(), id);
                    view.success("Contacto eliminado.");
                })));
    }
}
