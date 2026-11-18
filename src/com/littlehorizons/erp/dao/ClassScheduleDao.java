package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.enums.WeekDay;

import java.time.LocalTime;
import java.util.List;

/** Class blocks (horarios_clase) and overlap detection. */
public interface ClassScheduleDao {

    int insert(ClassSchedule s);

    boolean delete(int id);

    List<ClassSchedule> findByTeacher(int teacherId, int year);

    List<ClassSchedule> findByGroup(int gradeId, String section, int year);

    List<ClassSchedule> findByAssignment(int assignmentId);

    /**
     * Blocks of the same year and day that overlap [start, end) and share the teacher, the group
     * (grade + section) or the classroom.
     */
    List<ClassSchedule> findOverlapping(int year, WeekDay day, LocalTime start, LocalTime end, int teacherId, int gradeId, String section, String classroom);
}
