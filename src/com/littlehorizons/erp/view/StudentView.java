package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.EmergencyContact;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.GradeLevel;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.GuardianLink;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.Gender;
import com.littlehorizons.erp.util.Formatter;

import java.util.List;
import java.util.Map;

import static com.littlehorizons.erp.view.ConsoleView.row;

/** Forms and listings of students, guardians, emergency contacts and enrollments. */
public class StudentView {

    private final ConsoleView console;

    public StudentView(ConsoleView console) {
        this.console = console;
    }

    /** Reads the student form; with {@code current} the user can keep each value pressing Enter. */
    public Student readStudent(Student current) {
        console.section(current == null ? "Datos del estudiante" : "Actualizar estudiante (Enter conserva el valor)");
        boolean isNew = current == null;
        DocumentType type = console.chooseEnum("Tipo de documento", DocumentType.values(), isNew ? DocumentType.TI : current.documentType());
        String document = console.readText("Número de documento", isNew ? null : current.documentNumber());
        String first = console.readText("Primer nombre", isNew ? null : current.firstName());
        String middle = console.readOptional("Segundo nombre", isNew ? null : current.middleName());
        String last = console.readText("Primer apellido", isNew ? null : current.lastName());
        String secondLast = console.readOptional("Segundo apellido", isNew ? null : current.secondLastName());
        var birth = console.readDate("Fecha de nacimiento", isNew ? null : current.birthDate());
        Gender gender = console.chooseEnum("Género", Gender.values(), isNew ? null : current.gender());
        String address = console.readOptional("Dirección", isNew ? null : current.address());
        String phone = console.readOptional("Teléfono", isNew ? null : current.phone());
        String email = console.readOptional("Correo electrónico", isNew ? null : current.email());
        String blood = console.readOptional("Tipo de sangre (ej: O+)", isNew ? null : current.bloodType());
        String allergies = console.readOptional("Alergias", isNew ? null : current.allergies());
        String medical = console.readOptional("Condiciones médicas", isNew ? null : current.medicalConditions());
        var admission = console.readDate("Fecha de ingreso", isNew ? java.time.LocalDate.now() : current.admissionDate());
        return new Student(isNew ? null : current.id(), type, document, first, middle, last, secondLast, birth, gender, address,
                phone, email, blood, allergies, medical, admission, isNew ? null : current.status());
    }

    public Guardian readGuardian(Guardian current) {
        console.section(current == null ? "Datos del acudiente" : "Actualizar acudiente (Enter conserva el valor)");
        boolean isNew = current == null;
        DocumentType type = console.chooseEnum("Tipo de documento", DocumentType.adultTypes(), isNew ? DocumentType.CC : current.documentType());
        String document = console.readText("Número de documento", isNew ? null : current.documentNumber());
        String first = console.readText("Primer nombre", isNew ? null : current.firstName());
        String middle = console.readOptional("Segundo nombre", isNew ? null : current.middleName());
        String last = console.readText("Primer apellido", isNew ? null : current.lastName());
        String secondLast = console.readOptional("Segundo apellido", isNew ? null : current.secondLastName());
        String relationship = console.readText("Parentesco (Madre, Padre, Abuelo, Tutor Legal...)", isNew ? null : current.relationship());
        String phone = console.readText("Teléfono principal", isNew ? null : current.mainPhone());
        String phone2 = console.readOptional("Teléfono secundario", isNew ? null : current.secondaryPhone());
        String email = console.readOptional("Correo electrónico", isNew ? null : current.email());
        String address = console.readOptional("Dirección", isNew ? null : current.address());
        String occupation = console.readOptional("Ocupación", isNew ? null : current.occupation());
        boolean financial = console.readBoolean("¿Es responsable financiero?", isNew || current.financialResponsible());
        return new Guardian(isNew ? null : current.id(), type, document, first, middle, last, secondLast, relationship, phone,
                phone2, email, address, occupation, financial);
    }

    public EmergencyContact readContact(int studentId) {
        console.section("Nuevo contacto de emergencia");
        return new EmergencyContact(null, studentId, console.readRequired("Nombre completo"),
                console.readRequired("Parentesco (Abuela, Tío, Vecino...)"), console.readRequired("Teléfono"),
                console.readOptional("Teléfono alterno", null), console.readOptional("Observaciones", null));
    }

    public void showStudents(List<Student> students) {
        console.table(List.of("ID", "Documento", "Nombre completo", "Nacimiento", "Edad", "Estado"),
                students.stream().map(s -> row(s.id(), s.documentType() + " " + s.documentNumber(), s.fullName(),
                        Formatter.date(s.birthDate()), s.age(), s.status())).toList());
    }

    public void showStudentDetail(Student s, List<GuardianLink> guardians, List<EmergencyContact> contacts, List<Enrollment> enrollments) {
        console.section("Ficha del estudiante #" + s.id());
        console.field("Nombre", s.fullName());
        console.field("Documento", s.documentType() + " " + s.documentNumber());
        console.field("Nacimiento / edad", Formatter.date(s.birthDate()) + " (" + s.age() + " años)");
        console.field("Género", s.gender());
        console.field("Dirección", s.address());
        console.field("Teléfono / correo", Formatter.orDash(s.phone()) + " / " + Formatter.orDash(s.email()));
        console.field("Tipo de sangre", s.bloodType());
        console.field("Alergias", s.allergies());
        console.field("Condiciones médicas", s.medicalConditions());
        console.field("Fecha de ingreso", Formatter.date(s.admissionDate()));
        console.field("Estado", s.status());
        console.section("Acudientes");
        showGuardianLinks(guardians);
        console.section("Contactos de emergencia adicionales");
        showContacts(contacts);
        console.section("Matrículas");
        showEnrollments(enrollments);
    }

    public void showGuardianLinks(List<GuardianLink> links) {
        console.table(List.of("ID", "Nombre", "Parentesco", "Teléfono", "Correo", "Principal", "Emergencia", "Resp. financiero"),
                links.stream().map(l -> row(l.guardian().id(), l.guardian().fullName(), l.guardian().relationship(),
                        l.guardian().mainPhone(), l.guardian().email(), Formatter.yesNo(l.primary()),
                        Formatter.yesNo(l.emergencyContact()), Formatter.yesNo(l.guardian().financialResponsible()))).toList());
    }

    public void showContacts(List<EmergencyContact> contacts) {
        console.table(List.of("ID", "Nombre", "Parentesco", "Teléfono", "Alterno", "Observaciones"),
                contacts.stream().map(c -> row(c.id(), c.fullName(), c.relationship(), c.phone(), c.alternatePhone(), c.notes())).toList());
    }

    public void showEnrollments(List<Enrollment> enrollments) {
        console.table(List.of("ID", "Año", "Grado", "Estado", "Fecha matrícula", "Fecha retiro", "Observaciones"),
                enrollments.stream().map(e -> row(e.id(), e.academicYear(), e.gradeName(), e.status(),
                        Formatter.date(e.enrollmentDate()), Formatter.date(e.withdrawalDate()), e.notes())).toList());
    }

    public void showEnrollmentSummary(int year, Map<String, Map<String, Long>> countsByGradeAndStatus) {
        console.section("Resumen de matrícula " + year);
        console.table(List.of("Grado", "Activa", "Completada", "Retirada", "Total"),
                countsByGradeAndStatus.entrySet().stream().map(e -> {
                    Map<String, Long> c = e.getValue();
                    long total = c.values().stream().mapToLong(Long::longValue).sum();
                    return row(e.getKey(), c.getOrDefault("Activa", 0L), c.getOrDefault("Completada", 0L),
                            c.getOrDefault("Retirada", 0L), total);
                }).toList());
    }

    public void showGuardians(List<Guardian> guardians) {
        console.table(List.of("ID", "Documento", "Nombre", "Parentesco", "Teléfono", "Correo", "Resp. financiero"),
                guardians.stream().map(g -> row(g.id(), g.documentType() + " " + g.documentNumber(), g.fullName(), g.relationship(),
                        g.mainPhone(), g.email(), Formatter.yesNo(g.financialResponsible()))).toList());
    }

    public void showGuardianDetail(Guardian g, List<Student> students) {
        console.section("Acudiente #" + g.id());
        console.field("Nombre", g.fullName());
        console.field("Documento", g.documentType() + " " + g.documentNumber());
        console.field("Parentesco", g.relationship());
        console.field("Teléfonos", g.mainPhone() + (g.secondaryPhone() == null ? "" : " / " + g.secondaryPhone()));
        console.field("Correo", g.email());
        console.field("Dirección", g.address());
        console.field("Ocupación", g.occupation());
        console.field("Responsable financiero", Formatter.yesNo(g.financialResponsible()));
        console.section("Estudiantes a cargo");
        showStudents(students);
    }

    public void showGrades(List<GradeLevel> grades) {
        console.table(List.of("ID", "Grado", "Nivel"), grades.stream().map(g -> row(g.id(), g.name(), g.level())).toList());
    }
}
