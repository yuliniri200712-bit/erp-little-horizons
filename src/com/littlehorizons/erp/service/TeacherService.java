package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.ClassScheduleDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TeachingAssignmentDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeachingAssignment;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.Shift;
import com.littlehorizons.erp.model.enums.TeacherStatus;
import com.littlehorizons.erp.model.enums.WeekDay;
import com.littlehorizons.erp.util.Formatter;
import com.littlehorizons.erp.util.Validator;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Teacher directory, teacher-subject assignments (many-to-many per group and shift) and class
 * schedules with basic conflict detection (same teacher, same group or same classroom).
 */
public class TeacherService {

    private static final LocalTime MORNING_START = LocalTime.of(6, 0);
    private static final LocalTime MORNING_END = LocalTime.of(13, 30);
    private static final LocalTime AFTERNOON_START = LocalTime.of(12, 0);
    private static final LocalTime AFTERNOON_END = LocalTime.of(18, 30);
    private static final Duration MIN_BLOCK = Duration.ofMinutes(30);

    private final TeacherDao teacherDao;
    private final TeachingAssignmentDao assignmentDao;
    private final ClassScheduleDao scheduleDao;
    private final SubjectDao subjectDao;

    public TeacherService(TeacherDao teacherDao, TeachingAssignmentDao assignmentDao, ClassScheduleDao scheduleDao,
                          SubjectDao subjectDao) {
        this.teacherDao = teacherDao;
        this.assignmentDao = assignmentDao;
        this.scheduleDao = scheduleDao;
        this.subjectDao = subjectDao;
    }

    // ================================================================ directory

    public List<Teacher> listTeachers() {
        return teacherDao.findAll();
    }

    public List<Teacher> searchTeachers(String text) {
        return teacherDao.search(Validator.requireText(text, "Texto de búsqueda", 100));
    }

    public Teacher getTeacher(int id) {
        return teacherDao.findById(id).orElseThrow(() -> new BusinessException("No existe un profesor con ID " + id + "."));
    }

    public Teacher findTeacher(String idOrDocument) {
        String key = Validator.requireText(idOrDocument, "ID o documento", 20);
        Optional<Teacher> byDocument = teacherDao.findByDocument(key);
        if (byDocument.isPresent()) {
            return byDocument.get();
        }
        if (key.matches("\\d{1,9}")) {
            return getTeacher(Integer.parseInt(key));
        }
        throw new BusinessException("No se encontró un profesor con ID o documento '" + key + "'.");
    }

    public int createTeacher(Teacher teacher) {
        return teacherDao.insert(validate(teacher, null));
    }

    public void updateTeacher(Teacher teacher) {
        getTeacher(teacher.id());
        teacherDao.update(validate(teacher, teacher.id()));
    }

    public void deleteTeacher(int teacherId) {
        getTeacher(teacherId);
        if (teacherDao.countBlockingReferences(teacherId) > 0) {
            throw new BusinessException("El profesor tiene notas, asistencias, clubes o eventos asociados. Cambie su estado a 'Inactivo'.");
        }
        teacherDao.delete(teacherId);
    }

    private Teacher validate(Teacher t, Integer currentId) {
        String document = Validator.requireDocument(t.documentNumber());
        teacherDao.findByDocument(document).filter(o -> !o.id().equals(currentId)).ifPresent(o -> {
            throw new BusinessException("Ya existe un profesor con el documento " + document + ".");
        });
        String email = Validator.requireEmail(t.email());
        teacherDao.findAll().stream()
                .filter(o -> o.email().equalsIgnoreCase(email) && !o.id().equals(currentId))
                .findAny()
                .ifPresent(o -> {
                    throw new BusinessException("El correo " + email + " ya pertenece a " + o.fullName() + ".");
                });
        if (t.documentType() == null || !Arrays.asList(DocumentType.adultTypes()).contains(t.documentType())) {
            throw new BusinessException("El profesor debe identificarse con CC, CE o Pasaporte.");
        }
        if (t.hireDate() == null) {
            throw new BusinessException("La fecha de contratación es obligatoria.");
        }
        Validator.requireNotFuture(t.hireDate(), "Fecha de contratación");
        return new Teacher(t.id(), t.documentType(), document,
                Validator.requireText(t.firstName(), "Primer nombre", 100),
                Validator.optionalText(t.middleName(), "Segundo nombre", 100),
                Validator.requireText(t.lastName(), "Primer apellido", 100),
                Validator.optionalText(t.secondLastName(), "Segundo apellido", 100),
                email, Validator.optionalPhone(t.phone()),
                Validator.requireText(t.specialization(), "Especialización", 100),
                t.hireDate(), t.status() == null ? TeacherStatus.ACTIVE : t.status());
    }

    // ================================================================ assignments

    public int assign(int teacherId, int subjectId, int year, String section, Shift shift) {
        Teacher teacher = getTeacher(teacherId);
        if (teacher.status() != TeacherStatus.ACTIVE) {
            throw new BusinessException("Solo se asignan materias a profesores activos.");
        }
        Subject subject = subjectDao.findById(subjectId)
                .orElseThrow(() -> new BusinessException("No existe una materia con ID " + subjectId + "."));
        if (!subject.active()) {
            throw new BusinessException("La materia " + subject.code() + " está inactiva.");
        }
        int currentYear = LocalDate.now().getYear();
        Validator.requireRange(year, currentYear - 1, currentYear + 1, "Año académico");
        String group = section == null || section.isBlank() ? "A" : section.trim().toUpperCase();
        if (!group.matches("^[A-Z]$")) {
            throw new BusinessException("La sección debe ser una letra (A, B, C...).");
        }
        assignmentDao.findBySubject(subjectId, year).stream()
                .filter(a -> a.section().equals(group))
                .findAny()
                .ifPresent(a -> {
                    throw new BusinessException(subject.code() + " sección " + group + " ya está asignada a " + a.teacherName() + ".");
                });
        return assignmentDao.insert(new TeachingAssignment(null, teacherId, null, subjectId, null, null, subject.gradeId(),
                null, year, group, shift == null ? Shift.MORNING : shift));
    }

    public TeachingAssignment getAssignment(int id) {
        return assignmentDao.findById(id).orElseThrow(() -> new BusinessException("No existe la asignación " + id + "."));
    }

    public void removeAssignment(int assignmentId) {
        getAssignment(assignmentId);
        assignmentDao.delete(assignmentId);
    }

    public List<TeachingAssignment> assignmentsOf(int teacherId, Integer year) {
        return assignmentDao.findByTeacher(teacherId, year);
    }

    public List<TeachingAssignment> assignmentsByYear(int year) {
        return assignmentDao.findByYear(year);
    }

    public List<TeachingAssignment> assignmentsOfSubject(int subjectId, int year) {
        return assignmentDao.findBySubject(subjectId, year);
    }

    // ================================================================ schedules

    /**
     * Adds a class block validating the shift window and rejecting overlaps with blocks of the same
     * teacher, the same group (grade + section) or the same classroom in that year and day.
     */
    public int addSchedule(int assignmentId, WeekDay day, LocalTime start, LocalTime end, String classroom) {
        TeachingAssignment assignment = getAssignment(assignmentId);
        if (day == null || start == null || end == null) {
            throw new BusinessException("Día, hora de inicio y hora de fin son obligatorios.");
        }
        if (!end.isAfter(start)) {
            throw new BusinessException("La hora de fin debe ser posterior a la hora de inicio.");
        }
        if (Duration.between(start, end).compareTo(MIN_BLOCK) < 0) {
            throw new BusinessException("Un bloque de clase dura mínimo 30 minutos.");
        }
        validateShiftWindow(assignment.shift(), start, end);
        String room = Validator.optionalText(classroom, "Aula", 50);
        List<ClassSchedule> conflicts = scheduleDao.findOverlapping(assignment.academicYear(), day, start, end,
                assignment.teacherId(), assignment.gradeId(), assignment.section(), room);
        if (!conflicts.isEmpty()) {
            String detail = conflicts.stream()
                    .map(c -> describeConflict(c, assignment, room))
                    .collect(Collectors.joining(System.lineSeparator() + "  - ", "  - ", ""));
            throw new BusinessException("Conflicto de horario el " + day + " " + Formatter.time(start) + "-"
                    + Formatter.time(end) + ":" + System.lineSeparator() + detail);
        }
        return scheduleDao.insert(new ClassSchedule(null, assignmentId, day, start, end, room, null, null, null, null));
    }

    private static void validateShiftWindow(Shift shift, LocalTime start, LocalTime end) {
        LocalTime from = shift == Shift.AFTERNOON ? AFTERNOON_START : MORNING_START;
        LocalTime to = shift == Shift.AFTERNOON ? AFTERNOON_END : MORNING_END;
        if (start.isBefore(from) || end.isAfter(to)) {
            throw new BusinessException("La jornada " + shift + " va de " + Formatter.time(from) + " a " + Formatter.time(to) + ".");
        }
    }

    private static String describeConflict(ClassSchedule c, TeachingAssignment a, String room) {
        String when = Formatter.time(c.startTime()) + "-" + Formatter.time(c.endTime());
        String what = c.subjectName() + " de " + c.gradeName() + " " + c.section() + " (" + c.teacherName() + ")";
        if (c.teacherName() != null && c.teacherName().equals(a.teacherName())) {
            return "El profesor ya dicta " + what + " a las " + when;
        }
        if (c.gradeName().equals(a.gradeName()) && c.section().equals(a.section())) {
            return "El grupo ya tiene " + what + " a las " + when;
        }
        return "El aula " + room + " está ocupada por " + what + " a las " + when;
    }

    public void deleteSchedule(int scheduleId) {
        if (!scheduleDao.delete(scheduleId)) {
            throw new BusinessException("No existe el bloque de horario " + scheduleId + ".");
        }
    }

    public List<ClassSchedule> scheduleOfTeacher(int teacherId, int year) {
        return scheduleDao.findByTeacher(teacherId, year);
    }

    public List<ClassSchedule> scheduleOfGroup(int gradeId, String section, int year) {
        return scheduleDao.findByGroup(gradeId, section == null || section.isBlank() ? "A" : section.trim().toUpperCase(), year);
    }

    public List<ClassSchedule> scheduleOfAssignment(int assignmentId) {
        return scheduleDao.findByAssignment(assignmentId);
    }
}
