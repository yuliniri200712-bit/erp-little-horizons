package com.littlehorizons.erp.model.report;

import java.math.BigDecimal;

/** General figures shown in the statistics summary. */
public record DashboardStats(
        long activeStudents,
        long guardians,
        long activeTeachers,
        long activeSubjects,
        long activeClubs,
        long upcomingEvents,
        BigDecimal collectedThisMonth,
        BigDecimal overdueDebt,
        long overdueLoans,
        long availableResources) {
}
