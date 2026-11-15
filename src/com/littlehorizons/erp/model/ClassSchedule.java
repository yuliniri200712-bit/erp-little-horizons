package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.WeekDay;
import java.time.LocalTime;

/** Class block (day and hours) of an assignment. Table: horarios_clase. */
public record ClassSchedule(
        Integer id,
        int assignmentId,
        WeekDay day,
        LocalTime startTime,
        LocalTime endTime,
        String classroom,
        String teacherName,
        String subjectName,
        String gradeName,
        String section) {
}
