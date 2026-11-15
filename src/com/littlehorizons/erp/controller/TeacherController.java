package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.GradeLevel;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeachingAssignment;
import com.littlehorizons.erp.model.enums.Shift;
import com.littlehorizons.erp.model.enums.WeekDay;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.TeacherView;

import java.time.LocalTime;
import java.util.List;

/** Menu 3 - Gestión de Profesores: directory, subject assignments and class schedules. */
public class TeacherController extends BaseController {

    private final TeacherService service;
    private final TeacherView teacherView;
    private final EntityPicker picker;

    public TeacherController(ConsoleView view, TeacherService service, EntityPicker picker) {
        super(view);
        this.service = service;
        this.teacherView = new TeacherView(view);
        this.picker = picker;
    }

    @Override
    public void start() {
        runMenu("GESTIÓN DE PROFESORES", List.of(
                Option.action("Listar profesores (directorio)", () -> teacherView.showTeachers(service.listTeachers())),
                Option.action("Buscar profesor (nombre, documento o especialización)", () ->
                        teacherView.showTeachers(service.searchTeachers(view.readRequired("Texto a buscar")))),
                Option.action("Ver ficha del profesor y sus asignaciones", () -> {
                    Teacher t = picker.teacher();
                    teacherView.showTeacherDetail(t, service.assignmentsOf(t.id(), null));
                }),
                Option.action("Registrar profesor", () -> {
                    int id = service.createTeacher(teacherView.readTeacher(null));
                    view.success("Profesor registrado con ID " + id + ".");
                }),
                Option.action("Actualizar profesor", () -> {
                    service.updateTeacher(teacherView.readTeacher(picker.teacher()));
                    view.success("Profesor actualizado.");
                }),
                Option.action("Eliminar profesor", () -> {
                    Teacher t = picker.teacher();
                    if (view.confirm("¿Confirma eliminar a " + t.fullName() + "?")) {
                        service.deleteTeacher(t.id());
                        view.success("Profesor eliminado.");
                    }
                }),
                Option.action("Asignar materia a profesor (grupo y jornada)", this::assign),
                Option.action("Listar asignaciones de un año", () -> teacherView.showAssignments(service.assignmentsByYear(readYear()))),
                Option.action("Eliminar asignación", () -> {
                    Teacher t = picker.teacher();
                    teacherView.showAssignments(service.assignmentsOf(t.id(), null));
                    int id = view.readInt("ID de la asignación", 1, Integer.MAX_VALUE);
                    if (view.confirm("Se eliminarán también sus bloques de horario. ¿Continuar?")) {
                        service.removeAssignment(id);
                        view.success("Asignación eliminada.");
                    }
                }),
                Option.action("Agregar bloque de horario (día y horas)", this::addSchedule),
                Option.action("Ver horario de un profesor", () -> {
                    Teacher t = picker.teacher();
                    teacherView.showSchedule(service.scheduleOfTeacher(t.id(), readYear()));
                }),
                Option.action("Ver horario de un grupo (grado y sección)", () -> {
                    GradeLevel grade = picker.grade();
                    String section = view.readText("Sección", "A");
                    teacherView.showSchedule(service.scheduleOfGroup(grade.id(), section, readYear()));
                }),
                Option.action("Eliminar bloque de horario", () -> {
                    Teacher t = picker.teacher();
                    teacherView.showSchedule(service.scheduleOfTeacher(t.id(), readYear()));
                    service.deleteSchedule(view.readInt("ID del bloque", 1, Integer.MAX_VALUE));
                    view.success("Bloque eliminado.");
                })));
    }

    private void assign() {
        Teacher teacher = picker.teacher();
        Subject subject = picker.subject();
        int year = readYear();
        String section = view.readText("Sección / grupo", "A");
        Shift shift = view.chooseEnum("Jornada", Shift.values(), Shift.MORNING);
        int id = service.assign(teacher.id(), subject.id(), year, section, shift);
        view.success("Asignación #" + id + ": " + teacher.fullName() + " dicta " + subject.name() + " (" + subject.gradeName()
                + " " + section.toUpperCase() + ", " + shift + ").");
        if (view.confirm("¿Desea registrar ahora su horario?")) {
            addBlocks(id);
        }
    }

    private void addSchedule() {
        Teacher teacher = picker.teacher();
        List<TeachingAssignment> assignments = service.assignmentsOf(teacher.id(), readYear());
        teacherView.showAssignments(assignments);
        int id = view.readInt("ID de la asignación", 1, Integer.MAX_VALUE);
        addBlocks(service.getAssignment(id).id());
    }

    private void addBlocks(int assignmentId) {
        do {
            WeekDay day = view.chooseEnum("Día", WeekDay.values(), null);
            LocalTime start = view.readTime("Hora de inicio");
            LocalTime end = view.readTime("Hora de fin");
            String room = view.readOptional("Aula", null);
            execute("Agregar horario", () -> {
                int blockId = service.addSchedule(assignmentId, day, start, end, room);
                view.success("Bloque #" + blockId + " agregado: " + day + " " + start + "-" + end + ".");
            });
        } while (view.confirm("¿Agregar otro bloque para esta asignación?"));
        teacherView.showSchedule(service.scheduleOfAssignment(assignmentId));
    }
}
