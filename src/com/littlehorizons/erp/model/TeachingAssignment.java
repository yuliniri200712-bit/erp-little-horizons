package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.Shift;

/** Teacher assigned to a subject for a group (section) and shift. Table: asignaciones_profesor. */
public record TeachingAssignment(
        Integer id,
        int teacherId,
        String teacherName,
        int subjectId,
        String subjectCode,
        String subjectName,
        int gradeId,
        String gradeName,
        int academicYear,
        String section,
        Shift shift) {
}
