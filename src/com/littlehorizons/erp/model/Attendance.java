package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.AttendanceStatus;
import java.time.LocalDate;

/** Daily attendance record per student and subject. Table: asistencias. */
public record Attendance(
        Integer id,
        int studentId,
        String studentName,
        int subjectId,
        String subjectName,
        LocalDate date,
        AttendanceStatus status,
        String notes,
        Integer recordedBy) {
}
