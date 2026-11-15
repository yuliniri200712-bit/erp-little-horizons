package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GradeLevelDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TeachingAssignmentDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.enums.TeacherStatus;
import com.littlehorizons.erp.model.report.AcademicHistoryYear;
import com.littlehorizons.erp.model.report.CourseAverage;
import com.littlehorizons.erp.model.report.PeriodAverage;
import com.littlehorizons.erp.model.report.StudentRanking;
import com.littlehorizons.erp.model.report.SubjectAverage;
import com.littlehorizons.erp.util.TransactionManager;
import com.littlehorizons.erp.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Use cases of subjects, subject enrollment per period and grades. Averages are computed with
 * the Stream API (per subject, per period and general) and with SQL aggregations (course averages).
 */
public class AcademicService {

    private static final String SUBJECT_CODE_PATTERN = "^[A-Z]{2,6}-\\d{2}$";

    private final SubjectDao subjectDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;
    private final AcademicRecordDao recordDao;
    private final EnrollmentDao enrollmentDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;
    private final TeachingAssignmentDao assignmentDao;
    private final GradeLevelDao gradeDao;

    public AcademicService(SubjectDao subjectDao, SubjectEnrollmentDao subjectEnrollmentDao, AcademicRecordDao recordDao,
                           EnrollmentDao enrollmentDao, StudentDao studentDao, TeacherDao teacherDao,
                           TeachingAssignmentDao assignmentDao, GradeLevelDao gradeDao) {
        this.subjectDao = subjectDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
        this.recordDao = recordDao;
        this.enrollmentDao = enrollmentDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
        this.assignmentDao = assignmentDao;
        this.gradeDao = gradeDao;
    }

    // ================================================================ subjects

    public List<Subject> listSubjects() {
        return subjectDao.findAll();
    }

    public List<Subject> subjectsOfGrade(int gradeId) {
        return subjectDao.findByGrade(gradeId, false);
    }

    public Subject getSubject(int id) {
        return subjectDao.findById(id).orElseThrow(() -> new BusinessException("No existe una materia con ID " + id + "."));
    }

    /** Finds a subject by code (e.g. MAT-05) or by numeric ID. */
    public Subject findSubject(String idOrCode) {
        String key = Validator.requireText(idOrCode, "ID o código de materia", 20).toUpperCase();
        Optional<Subject> byCode = subjectDao.findByCode(key);
        if (byCode.isPresent()) {
            return byCode.get();
        }
        if (key.matches("\\d{1,9}")) {
            return getSubject(Integer.parseInt(key));
        }
        throw new BusinessException("No se encontró la materia '" + key + "'.");
    }

    public int createSubject(Subject subject) {
        return subjectDao.insert(validateSubject(subject, null));
    }

    public void updateSubject(Subject subject) {
        getSubject(subject.id());
        subjectDao.update(validateSubject(subject, subject.id()));
    }

    public void deleteSubject(int subjectId) {
        getSubject(subjectId);
        if (subjectDao.countDependencies(subjectId) > 0) {
            throw new BusinessException("La materia tiene notas, inscripciones o asistencias. Desactívela en lugar de eliminarla.");
        }
        subjectDao.delete(subjectId);
    }

    private Subject validateSubject(Subject s, Integer currentId) {
        String code = Validator.requireText(s.code(), "Código", 20).toUpperCase();
        if (!code.matches(SUBJECT_CODE_PATTERN)) {
            throw new BusinessException("El código debe tener el formato AREA-NN (ejemplo: MAT-05).");
        }
        subjectDao.findByCode(code).filter(other -> !other.id().equals(currentId)).ifPresent(other -> {
            throw new BusinessException("Ya existe una materia con el código " + code + ".");
        });
        String gradeName = gradeDao.findById(s.gradeId())
                .orElseThrow(() -> new BusinessException("El grado " + s.gradeId() + " no existe.")).name();
        Validator.requireRange(s.weeklyHours(), 1, 40, "Horas semanales");
        return new Subject(s.id(), code, Validator.requireText(s.name(), "Nombre", 100),
                Validator.optionalText(s.description(), "Descripción", 1000), s.gradeId(), gradeName, s.weeklyHours(), s.active());
    }

    // ================================================================ subject enrollment

    /** Enrolls a student in a subject of the grade of his/her active enrollment of that year. */
    public int enrollInSubject(int studentId, int subjectId, int year, AcademicPeriod period, String section) {
        Student student = requireActiveStudent(studentId);
        Subject subject = getSubject(subjectId);
        validateSubjectEnrollment(student, subject, year, period);
        if (subjectEnrollmentDao.exists(studentId, subjectId, year, period)) {
            throw new BusinessException("El estudiante ya está inscrito en " + subject.name() + " para el período " + period + ".");
        }
        return subjectEnrollmentDao.insert(new SubjectEnrollment(null, studentId, null, subjectId, null, year, period,
                normalizeSection(section), LocalDate.now(), EnrollmentStatus.ACTIVE));
    }

    /** Enrolls the student in every active subject of the grade; returns how many were created. */
    public int enrollInAllSubjects(int studentId, int year, AcademicPeriod period, String section) {
        Student student = requireActiveStudent(studentId);
        Enrollment enrollment = requireActiveEnrollment(studentId, year);
        List<Subject> pending = subjectDao.findByGrade(enrollment.gradeId(), true).stream()
                .filter(subject -> !subjectEnrollmentDao.exists(studentId, subject.id(), year, period))
                .toList();
        if (pending.isEmpty()) {
            throw new BusinessException("El estudiante ya está inscrito en todas las materias de " + enrollment.gradeName()
                    + " para el período " + period + ".");
        }
        return TransactionManager.inTransaction(() -> {
            pending.forEach(subject -> {
                validateSubjectEnrollment(student, subject, year, period);
                subjectEnrollmentDao.insert(new SubjectEnrollment(null, studentId, null, subject.id(), null, year, period,
                        normalizeSection(section), LocalDate.now(), EnrollmentStatus.ACTIVE));
            });
            return pending.size();
        });
    }

    public void withdrawSubjectEnrollment(int enrollmentId) {
        SubjectEnrollment enrollment = subjectEnrollmentDao.findById(enrollmentId)
                .orElseThrow(() -> new BusinessException("No existe la inscripción " + enrollmentId + "."));
        if (enrollment.status() == EnrollmentStatus.WITHDRAWN) {
            throw new BusinessException("La inscripción ya está retirada.");
        }
        subjectEnrollmentDao.updateStatus(enrollmentId, EnrollmentStatus.WITHDRAWN);
    }

    public List<SubjectEnrollment> subjectEnrollmentsOf(int studentId, int year) {
        return subjectEnrollmentDao.findByStudent(studentId, year);
    }

    public List<SubjectEnrollment> studentsInSubject(int subjectId, int year, AcademicPeriod period) {
        return subjectEnrollmentDao.findBySubject(subjectId, year, period);
    }

    private void validateSubjectEnrollment(Student student, Subject subject, int year, AcademicPeriod period) {
        if (period == null || !period.isRegular()) {
            throw new BusinessException("La inscripción se realiza en los períodos I, II, III o IV.");
        }
        if (!subject.active()) {
            throw new BusinessException("La materia " + subject.code() + " está inactiva.");
        }
        Enrollment enrollment = requireActiveEnrollment(student.id(), year);
        if (enrollment.gradeId() != subject.gradeId()) {
            throw new BusinessException("La materia " + subject.code() + " pertenece a " + subject.gradeName()
                    + " pero el estudiante está matriculado en " + enrollment.gradeName() + ".");
        }
    }

    private Enrollment requireActiveEnrollment(int studentId, int year) {
        return enrollmentDao.findByStudentAndYear(studentId, year)
                .filter(e -> e.status() == EnrollmentStatus.ACTIVE)
                .orElseThrow(() -> new BusinessException("El estudiante no tiene matrícula activa en el año " + year + "."));
    }

    private Student requireActiveStudent(int studentId) {
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new BusinessException("No existe un estudiante con ID " + studentId + "."));
        if (student.status() != StudentStatus.ACTIVE) {
            throw new BusinessException("El estudiante no está activo (estado: " + student.status() + ").");
        }
        return student;
    }

    private static String normalizeSection(String section) {
        String value = section == null || section.isBlank() ? "A" : section.trim().toUpperCase();
        if (!value.matches("^[A-Z]$")) {
            throw new BusinessException("La sección debe ser una letra (A, B, C...).");
        }
        return value;
    }

    // ================================================================ grades

    public BigDecimal parseScore(String raw) {
        return Validator.parseScore(raw);
    }

    public Optional<AcademicRecord> findGrade(int studentId, int subjectId, int year, AcademicPeriod period) {
        return recordDao.find(studentId, subjectId, year, period);
    }

    /**
     * Saves (inserts or updates) a grade. Rules: score 0.0-5.0 with one decimal, student enrolled in
     * the subject (in that period, or in any period for the Final grade) and teacher assigned to the
     * subject in that year.
     */
    public AcademicRecord saveGrade(int studentId, int subjectId, int teacherId, int year, AcademicPeriod period,
                                    BigDecimal score, String notes, LocalDate recordDate) {
        BigDecimal validScore = Validator.parseScore(score == null ? null : score.toPlainString());
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new BusinessException("No existe un estudiante con ID " + studentId + "."));
        if (student.status() == StudentStatus.WITHDRAWN) {
            throw new BusinessException("No se registran notas a estudiantes retirados.");
        }
        Subject subject = getSubject(subjectId);
        boolean enrolled = period.isRegular()
                ? subjectEnrollmentDao.findByStudent(studentId, year).stream()
                .anyMatch(e -> e.subjectId() == subjectId && e.period() == period && e.status() != EnrollmentStatus.WITHDRAWN)
                : subjectEnrollmentDao.isEnrolledInYear(studentId, subjectId, year);
        if (!enrolled) {
            throw new BusinessException("El estudiante no está inscrito en " + subject.name() + " (" + subject.code()
                    + ") para el período " + period + " de " + year + ".");
        }
        Teacher teacher = teacherDao.findById(teacherId)
                .orElseThrow(() -> new BusinessException("No existe un profesor con ID " + teacherId + "."));
        if (teacher.status() != TeacherStatus.ACTIVE) {
            throw new BusinessException("El profesor " + teacher.fullName() + " no está activo.");
        }
        if (!assignmentDao.isAssigned(teacherId, subjectId, year)) {
            throw new BusinessException("El profesor " + teacher.fullName() + " no tiene asignada la materia " + subject.code()
                    + " en " + year + ".");
        }
        LocalDate date = recordDate == null ? LocalDate.now() : recordDate;
        Validator.requireNotFuture(date, "Fecha de registro");
        String validNotes = Validator.optionalText(notes, "Observaciones", 1000);
        Optional<AcademicRecord> existing = recordDao.find(studentId, subjectId, year, period);
        AcademicRecord record = new AcademicRecord(existing.map(AcademicRecord::id).orElse(null), studentId, subjectId,
                subject.name(), teacherId, teacher.fullName(), year, period, validScore, validNotes, date);
        if (existing.isPresent()) {
            recordDao.update(record);
            return record;
        }
        int id = recordDao.insert(record);
        return new AcademicRecord(id, studentId, subjectId, subject.name(), teacherId, teacher.fullName(), year, period,
                validScore, validNotes, date);
    }

    public void deleteGrade(int recordId) {
        recordDao.findById(recordId).orElseThrow(() -> new BusinessException("No existe la nota con ID " + recordId + "."));
        recordDao.delete(recordId);
    }

    public List<AcademicRecord> gradesOf(int studentId, int year) {
        return recordDao.findByStudent(studentId, year);
    }

    /** Average of periods I-IV per subject plus the Final grade when registered. */
    public List<SubjectAverage> subjectAverages(int studentId, int year) {
        Map<String, List<AcademicRecord>> bySubject = recordDao.findByStudent(studentId, year).stream()
                .collect(Collectors.groupingBy(AcademicRecord::subjectName, TreeMap::new, Collectors.toList()));
        return bySubject.entrySet().stream()
                .map(entry -> {
                    List<AcademicRecord> regular = entry.getValue().stream().filter(r -> r.period().isRegular()).toList();
                    double average = regular.stream().mapToDouble(r -> r.score().doubleValue()).average().orElse(Double.NaN);
                    BigDecimal finalScore = entry.getValue().stream().filter(r -> !r.period().isRegular())
                            .map(AcademicRecord::score).findFirst().orElse(null);
                    return new SubjectAverage(entry.getKey(), average, regular.size(), finalScore);
                })
                .toList();
    }

    /** General average of the student in each period (I-IV). */
    public List<PeriodAverage> periodAverages(int studentId, int year) {
        Map<AcademicPeriod, List<AcademicRecord>> byPeriod = recordDao.findByStudent(studentId, year).stream()
                .filter(r -> r.period().isRegular())
                .collect(Collectors.groupingBy(AcademicRecord::period, () -> new EnumMap<>(AcademicPeriod.class), Collectors.toList()));
        return byPeriod.entrySet().stream()
                .map(e -> new PeriodAverage(e.getKey(),
                        e.getValue().stream().mapToDouble(r -> r.score().doubleValue()).average().orElse(Double.NaN),
                        e.getValue().size()))
                .toList();
    }

    public OptionalDouble generalAverage(int studentId, int year) {
        return recordDao.findByStudent(studentId, year).stream()
                .filter(r -> r.period().isRegular())
                .mapToDouble(r -> r.score().doubleValue())
                .average();
    }

    public List<CourseAverage> courseAverages(int gradeId, int year, AcademicPeriod period) {
        if (period != null && !period.isRegular()) {
            throw new BusinessException("Seleccione un período I-IV o todos.");
        }
        return recordDao.courseAverages(gradeId, year, period);
    }

    public List<StudentRanking> ranking(Integer gradeId, int year, AcademicPeriod period) {
        return recordDao.ranking(gradeId, year, period);
    }

    /** Complete academic history: every year with its grade, grades per period and averages. */
    public List<AcademicHistoryYear> academicHistory(int studentId) {
        Map<Integer, Enrollment> enrollments = enrollmentDao.findByStudent(studentId).stream()
                .collect(Collectors.toMap(Enrollment::academicYear, e -> e));
        Map<Integer, List<AcademicRecord>> byYear = recordDao.findByStudent(studentId, null).stream()
                .collect(Collectors.groupingBy(AcademicRecord::academicYear, TreeMap::new, Collectors.toList()));
        enrollments.keySet().forEach(year -> byYear.putIfAbsent(year, List.of()));
        return byYear.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                .map(entry -> {
                    Enrollment enrollment = enrollments.get(entry.getKey());
                    List<AcademicHistoryYear.Row> rows = entry.getValue().stream()
                            .collect(Collectors.groupingBy(AcademicRecord::subjectName, TreeMap::new, Collectors.toList()))
                            .entrySet().stream()
                            .map(subject -> toHistoryRow(subject.getKey(), subject.getValue()))
                            .toList();
                    OptionalDouble general = entry.getValue().stream().filter(r -> r.period().isRegular())
                            .mapToDouble(r -> r.score().doubleValue()).average();
                    if (general.isEmpty()) {
                        general = entry.getValue().stream().mapToDouble(r -> r.score().doubleValue()).average();
                    }
                    return new AcademicHistoryYear(entry.getKey(),
                            enrollment == null ? "-" : enrollment.gradeName(),
                            enrollment == null ? "-" : enrollment.status().label(),
                            rows, general.isPresent() ? general.getAsDouble() : null);
                })
                .toList();
    }

    private static AcademicHistoryYear.Row toHistoryRow(String subjectName, List<AcademicRecord> records) {
        Map<AcademicPeriod, BigDecimal> scores = new EnumMap<>(AcademicPeriod.class);
        records.forEach(r -> scores.put(r.period(), r.score()));
        OptionalDouble average = records.stream().filter(r -> r.period().isRegular())
                .mapToDouble(r -> r.score().doubleValue()).average();
        Double value = average.isPresent() ? average.getAsDouble()
                : scores.containsKey(AcademicPeriod.FINAL) ? scores.get(AcademicPeriod.FINAL).doubleValue() : null;
        return new AcademicHistoryYear.Row(subjectName, scores, value);
    }
}
