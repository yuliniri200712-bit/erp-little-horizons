package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.report.StudentParticipation;

import java.time.LocalDate;
import java.util.List;

/** Cross-module aggregate queries used by the statistics menu. */
public interface ReportDao {

    long countActiveStudents();

    long countGuardians();

    long countActiveTeachers();

    long countActiveSubjects();

    long countActiveClubs();

    long countUpcomingEvents(LocalDate from);

    long countOverdueLoans();

    /** Active clubs, events and podium places per student (active students only). */
    List<StudentParticipation> participationByStudent(int year);
}
