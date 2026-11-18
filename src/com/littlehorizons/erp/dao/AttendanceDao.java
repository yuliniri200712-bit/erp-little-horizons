package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.report.AbsenceAlert;
import com.littlehorizons.erp.model.report.RecurrentAbsence;

import java.time.LocalDate;
import java.util.List;

/** Daily attendance (asistencias) and absence reports. */
public interface AttendanceDao {

    /** Inserts the record or updates it when the student already has attendance for that subject and day. */
    void upsert(Attendance a);

    List<Attendance> findByStudent(int studentId, LocalDate from, LocalDate to);

    List<Attendance> findBySubjectAndDate(int subjectId, LocalDate date);

    /** Students absent on more than {@code maxDays} different days of the same (Monday-based) week. */
    List<AbsenceAlert> weeklyAbsenceAlerts(LocalDate from, LocalDate to, int maxDays);

    /** Students with at least {@code minAbsences} absences in the same subject within the range. */
    List<RecurrentAbsence> recurrentAbsences(LocalDate from, LocalDate to, int minAbsences);
}
