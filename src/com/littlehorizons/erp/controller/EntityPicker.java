package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.GradeLevel;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.service.AcademicService;
import com.littlehorizons.erp.service.ExtracurricularService;
import com.littlehorizons.erp.service.FinanceService;
import com.littlehorizons.erp.service.ResourceService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.ExtracurricularView;
import com.littlehorizons.erp.view.StudentView;
import com.littlehorizons.erp.view.TeacherView;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Shared selection of entities by ID, document/code or name search, reused by every controller
 * (avoids duplicating the same lookup dialogs). Typing 0 cancels the operation.
 */
public class EntityPicker {

    private final ConsoleView view;
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final AcademicService academicService;
    private final ExtracurricularService extracurricularService;
    private final FinanceService financeService;
    private final ResourceService resourceService;
    private final StudentView studentView;
    private final TeacherView teacherView;
    private final ExtracurricularView extracurricularView;

    public EntityPicker(ConsoleView view, StudentService studentService, TeacherService teacherService,
                        AcademicService academicService, ExtracurricularService extracurricularService,
                        FinanceService financeService, ResourceService resourceService) {
        this.view = view;
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.academicService = academicService;
        this.extracurricularService = extracurricularService;
        this.financeService = financeService;
        this.resourceService = resourceService;
        this.studentView = new StudentView(view);
        this.teacherView = new TeacherView(view);
        this.extracurricularView = new ExtracurricularView(view);
    }

    /**
     * Generic lookup: Enter starts a name search, 0 cancels, anything else is resolved by
     * {@code finder} (ID or document/code); errors are shown and the question repeated.
     */
    private <T> T pick(String prompt, Function<String, T> finder, Function<String, List<T>> search, Consumer<List<T>> printer) {
        return view.readValidated(prompt + " (Enter = buscar por nombre, 0 = cancelar)", key -> {
            if ("0".equals(key)) {
                throw new OperationCancelledException();
            }
            if (!key.isBlank()) {
                return finder.apply(key);
            }
            String text = view.readRequired("Texto a buscar");
            List<T> found = search.apply(text);
            if (found.isEmpty()) {
                throw new BusinessException("No hay coincidencias para '" + text + "'.");
            }
            printer.accept(found);
            String chosen = view.readRequired("Escriba el ID de la lista");
            if ("0".equals(chosen)) {
                throw new OperationCancelledException();
            }
            return finder.apply(chosen);
        });
    }

    public Student student() {
        Student student = pick("ID o documento del estudiante", studentService::findStudent, studentService::searchStudents,
                studentView::showStudents);
        view.info("  -> " + student.fullName() + " (" + student.status() + ")");
        return student;
    }

    public Guardian guardian() {
        Guardian guardian = pick("ID o documento del acudiente", studentService::findGuardian, studentService::searchGuardians,
                studentView::showGuardians);
        view.info("  -> " + guardian.fullName());
        return guardian;
    }

    public Teacher teacher() {
        Teacher teacher = pick("ID o documento del profesor", teacherService::findTeacher, teacherService::searchTeachers,
                teacherView::showTeachers);
        view.info("  -> " + teacher.fullName() + " (" + teacher.specialization() + ")");
        return teacher;
    }

    /** Optional teacher (Enter = none). */
    public Integer optionalTeacherId(String prompt) {
        Integer id = view.readOptionalInt(prompt + " - ID del profesor", 1, Integer.MAX_VALUE);
        if (id == null) {
            return null;
        }
        Teacher teacher = teacherService.getTeacher(id);
        view.info("  -> " + teacher.fullName());
        return id;
    }

    public Subject subject() {
        Subject subject = pick("ID o código de la materia (ej: MAT-05)", academicService::findSubject,
                text -> academicService.listSubjects().stream()
                        .filter(s -> (s.name() + " " + s.gradeName() + " " + s.code()).toLowerCase().contains(text.toLowerCase()))
                        .toList(),
                list -> list.forEach(s -> view.info("  " + s.id() + " - " + s.code() + " " + s.name() + " (" + s.gradeName() + ")")));
        view.info("  -> " + subject.code() + " " + subject.name() + " (" + subject.gradeName() + ")");
        return subject;
    }

    public GradeLevel grade() {
        List<GradeLevel> grades = studentService.grades();
        return view.chooseFrom("Grado", grades, GradeLevel::name);
    }

    public Club club() {
        List<Club> clubs = extracurricularService.listClubs();
        extracurricularView.showClubs(clubs);
        int id = view.readValidated("ID del club (0 = cancelar)", text -> {
            if ("0".equals(text)) {
                throw new OperationCancelledException();
            }
            return extracurricularService.getClub(Integer.parseInt(text)).id();
        });
        return extracurricularService.getClub(id);
    }

    public SchoolEvent event() {
        extracurricularView.showEvents(extracurricularService.listEvents());
        return view.readValidated("ID del evento (0 = cancelar)", text -> {
            if ("0".equals(text)) {
                throw new OperationCancelledException();
            }
            return extracurricularService.getEvent(Integer.parseInt(text));
        });
    }

    public PaymentConcept concept() {
        List<PaymentConcept> concepts = financeService.listConcepts();
        concepts.forEach(c -> view.info("  " + c.id() + " - " + c.code() + " " + c.name()));
        return view.readValidated("ID o código del concepto (0 = cancelar)", text -> {
            if ("0".equals(text)) {
                throw new OperationCancelledException();
            }
            return financeService.findConcept(text);
        });
    }

    public Resource resource() {
        Resource resource = pick("ID o código del recurso (ej: BIB-001)", resourceService::findResource,
                resourceService::searchResources,
                list -> list.forEach(r -> view.info("  " + r.id() + " - " + r.code() + " " + r.name() + " [" + r.status() + "]")));
        view.info("  -> " + resource.code() + " " + resource.name() + " [" + resource.status() + "]");
        return resource;
    }
}
