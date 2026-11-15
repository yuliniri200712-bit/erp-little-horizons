package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.TeachingAssignmentDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectEnrollment;
import com.littlehorizons.erp.model.enums.AttendanceStatus;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.report.AbsenceAlert;
import com.littlehorizons.erp.model.report.RecurrentAbsence;
import com.littlehorizons.erp.util.TransactionManager;
import com.littlehorizons.erp.util.Validator;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Daily attendance per student and subject, plus absence reports. The weekly alert flags
 * students absent on MORE THAN {@value #WEEKLY_ABSENCE_THRESHOLD} different days of one week.
 */
public class AttendanceService {

    public static final int WEEKLY_ABSENCE_THRESHOLD = 3;

    private final AttendanceDao attendanceDao;
    private final StudentDao studentDao;
    private final SubjectDao subjectDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;
    private final TeachingAssignmentDao assignmentDao;

    public AttendanceService(AttendanceDao attendanceDao, StudentDao studentDao, SubjectDao subjectDao,
                             SubjectEnrollmentDao subjectEnrollmentDao, TeachingAssignmentDao assignmentDao) {
        this.attendanceDao = attendanceDao;
        this.studentDao = studentDao;
        this.subjectDao = subjectDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
        this.assignmentDao = assignmentDao;
    }

    /** Registers (or corrects) the attendance of one student in one subject on one day. */
    public void record(int studentId, int subjectId, LocalDate date, AttendanceStatus status, String notes, Integer teacherId) {
        Subject subject = validateClass(subjectId, date, teacherId);
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new BusinessException("No existe un estudiante con ID " + studentId + "."));
        if (student.status() != StudentStatus.ACTIVE) {
            throw new BusinessException("El estudiante no está activo.");
        }
        if (!subjectEnrollmentDao.isEnrolledInYear(studentId, subjectId, date.getYear())) {
            throw new BusinessException("El estudiante no está inscrito en " + subject.name() + " en " + date.getYear() + ".");
        }
        if (status == null) {
            throw new BusinessException("El estado de asistencia es obligatorio.");
        }
        attendanceDao.upsert(new Attendance(null, studentId, null, subjectId, null, date, status,
                Validator.optionalText(notes, "Observaciones", 500), teacherId));
    }

    /** Students enrolled in the subject during the year of {@code date} (class roster). */
    public List<SubjectEnrollment> classRoster(int subjectId, LocalDate date) {
        validateClass(subjectId, date, null);
        List<SubjectEnrollment> roster = subjectEnrollmentDao.findStudentsOfSubjectInYear(subjectId, date.getYear());
        if (roster.isEmpty()) {
            throw new BusinessException("No hay estudiantes inscritos en la materia para " + date.getYear() + ".");
        }
        return roster;
    }

    /** Saves the attendance of a whole class in a single transaction. */
    public int recordClass(int subjectId, LocalDate date, Integer teacherId, Map<Integer, AttendanceStatus> statusByStudent) {
        validateClass(subjectId, date, teacherId);
        TransactionManager.inTransaction(() -> statusByStudent.forEach((studentId, status) ->
                attendanceDao.upsert(new Attendance(null, studentId, null, subjectId, null, date, status, null, teacherId))));
        return statusByStudent.size();
    }

    private Subject validateClass(int subjectId, LocalDate date, Integer teacherId) {
        Subject subject = subjectDao.findById(subjectId)
                .orElseThrow(() -> new BusinessException("No existe una materia con ID " + subjectId + "."));
        if (date == null) {
            throw new BusinessException("La fecha es obligatoria.");
        }
        Validator.requireNotFuture(date, "Fecha de asistencia");
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new BusinessException("No se registra asistencia en domingo.");
        }
        if (teacherId != null && !assignmentDao.isAssigned(teacherId, subjectId, date.getYear())) {
            throw new BusinessException("El profesor no tiene asignada la materia " + subject.code() + " en " + date.getYear() + ".");
        }
        return subject;
    }

    public List<Attendance> attendanceOf(int studentId, LocalDate from, LocalDate to) {
        Validator.requireOrder(from, to, "Desde", "Hasta");
        return attendanceDao.findByStudent(studentId, from, to);
    }

    /** Count of records per status, e.g. {Presente=40, Ausente=3}. */
    public Map<AttendanceStatus, Long> summarize(List<Attendance> records) {
        Map<AttendanceStatus, Long> summary = new EnumMap<>(AttendanceStatus.class);
        summary.putAll(records.stream().collect(Collectors.groupingBy(Attendance::status, Collectors.counting())));
        return summary;
    }

    public List<Attendance> attendanceOfClass(int subjectId, LocalDate date) {
        return attendanceDao.findBySubjectAndDate(subjectId, date);
    }

    /** Students absent on more than 3 different days in the same week within the range. */
    public List<AbsenceAlert> weeklyAbsenceAlerts(LocalDate from, LocalDate to) {
        Validator.requireOrder(from, to, "Desde", "Hasta");
        return attendanceDao.weeklyAbsenceAlerts(from, to, WEEKLY_ABSENCE_THRESHOLD);
    }

    public List<RecurrentAbsence> recurrentAbsences(LocalDate from, LocalDate to, int minAbsences) {
        Validator.requireOrder(from, to, "Desde", "Hasta");
        Validator.requireRange(minAbsences, 1, 100, "Mínimo de ausencias");
        return attendanceDao.recurrentAbsences(from, to, minAbsences);
    }

    /** Groups recurrent absences by student (total absences across subjects). */
    public Map<String, Integer> totalAbsencesByStudent(List<RecurrentAbsence> rows) {
        return rows.stream().collect(Collectors.groupingBy(RecurrentAbsence::studentName,
                TreeMap::new, Collectors.summingInt(RecurrentAbsence::absences)));
    }

    /** Helper for views: maps each student id of the roster to its name. */
    public Map<Integer, String> namesOf(List<SubjectEnrollment> roster) {
        return roster.stream().collect(Collectors.toMap(SubjectEnrollment::studentId, SubjectEnrollment::studentName,
                (a, b) -> a, LinkedHashMap::new));
    }
}
