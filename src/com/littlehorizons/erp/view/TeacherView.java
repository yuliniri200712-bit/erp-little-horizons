package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeachingAssignment;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.TeacherStatus;
import com.littlehorizons.erp.util.Formatter;

import java.time.LocalDate;
import java.util.List;

import static com.littlehorizons.erp.view.ConsoleView.row;

/** Forms and listings of teachers, assignments and class schedules. */
public class TeacherView {

    private final ConsoleView console;

    public TeacherView(ConsoleView console) {
        this.console = console;
    }

    public Teacher readTeacher(Teacher current) {
        boolean isNew = current == null;
        console.section(isNew ? "Datos del profesor" : "Actualizar profesor (Enter conserva el valor)");
        DocumentType type = console.chooseEnum("Tipo de documento", DocumentType.adultTypes(), isNew ? DocumentType.CC : current.documentType());
        String document = console.readText("Número de documento", isNew ? null : current.documentNumber());
        String first = console.readText("Primer nombre", isNew ? null : current.firstName());
        String middle = console.readOptional("Segundo nombre", isNew ? null : current.middleName());
        String last = console.readText("Primer apellido", isNew ? null : current.lastName());
        String secondLast = console.readOptional("Segundo apellido", isNew ? null : current.secondLastName());
        String email = console.readText("Correo electrónico", isNew ? null : current.email());
        String phone = console.readOptional("Teléfono", isNew ? null : current.phone());
        String specialization = console.readText("Especialización", isNew ? null : current.specialization());
        LocalDate hire = console.readDate("Fecha de contratación", isNew ? LocalDate.now() : current.hireDate());
        TeacherStatus status = console.chooseEnum("Estado", TeacherStatus.values(), isNew ? TeacherStatus.ACTIVE : current.status());
        return new Teacher(isNew ? null : current.id(), type, document, first, middle, last, secondLast, email, phone,
                specialization, hire, status);
    }

    public void showTeachers(List<Teacher> teachers) {
        console.table(List.of("ID", "Documento", "Nombre", "Especialización", "Correo", "Teléfono", "Estado"),
                teachers.stream().map(t -> row(t.id(), t.documentType() + " " + t.documentNumber(), t.fullName(), t.specialization(),
                        t.email(), t.phone(), t.status())).toList());
    }

    public void showTeacherDetail(Teacher t, List<TeachingAssignment> assignments) {
        console.section("Profesor #" + t.id());
        console.field("Nombre", t.fullName());
        console.field("Documento", t.documentType() + " " + t.documentNumber());
        console.field("Especialización", t.specialization());
        console.field("Correo", t.email());
        console.field("Teléfono", t.phone());
        console.field("Fecha de contratación", Formatter.date(t.hireDate()));
        console.field("Estado", t.status());
        console.section("Asignaciones");
        showAssignments(assignments);
    }

    public void showAssignments(List<TeachingAssignment> assignments) {
        console.table(List.of("ID", "Año", "Profesor", "Código", "Materia", "Grado", "Sección", "Jornada"),
                assignments.stream().map(a -> row(a.id(), a.academicYear(), a.teacherName(), a.subjectCode(), a.subjectName(),
                        a.gradeName(), a.section(), a.shift())).toList());
    }

    public void showSchedule(List<ClassSchedule> blocks) {
        console.table(List.of("ID", "Día", "Inicio", "Fin", "Materia", "Grado", "Sección", "Profesor", "Aula"),
                blocks.stream().map(b -> row(b.id(), b.day(), Formatter.time(b.startTime()), Formatter.time(b.endTime()),
                        b.subjectName(), b.gradeName(), b.section(), b.teacherName(), b.classroom())).toList());
    }
}
