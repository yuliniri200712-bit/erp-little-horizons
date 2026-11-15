package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.model.enums.ResourceStatus;
import com.littlehorizons.erp.model.report.DashboardStats;
import com.littlehorizons.erp.model.report.StudentParticipation;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.PaymentDao;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Cross-module figures for the "Reportes y Estadísticas" menu. Module-specific reports are
 * delegated to the corresponding services (single responsibility per module).
 */
public class ReportService {

    private final ReportDao reportDao;
    private final PaymentDao paymentDao;
    private final ResourceDao resourceDao;
    private final FinanceService financeService;
    private final ResourceService resourceService;

    public ReportService(ReportDao reportDao, PaymentDao paymentDao, ResourceDao resourceDao, FinanceService financeService,
                         ResourceService resourceService) {
        this.reportDao = reportDao;
        this.paymentDao = paymentDao;
        this.resourceDao = resourceDao;
        this.financeService = financeService;
        this.resourceService = resourceService;
    }

    public DashboardStats dashboard() {
        resourceService.refreshOverdue();
        LocalDate today = LocalDate.now();
        return new DashboardStats(
                reportDao.countActiveStudents(),
                reportDao.countGuardians(),
                reportDao.countActiveTeachers(),
                reportDao.countActiveSubjects(),
                reportDao.countActiveClubs(),
                reportDao.countUpcomingEvents(today),
                paymentDao.totalBetween(today.withDayOfMonth(1), today),
                financeService.totalPending(financeService.debtsOverOneMonth()),
                reportDao.countOverdueLoans(),
                resourceDao.countByStatus(ResourceStatus.AVAILABLE));
    }

    /** Participation of active students, most involved first. */
    public List<StudentParticipation> participationByStudent(int year) {
        return reportDao.participationByStudent(year).stream()
                .sorted(Comparator.comparingLong((StudentParticipation p) -> p.activeClubs() + p.events()).reversed()
                        .thenComparing(StudentParticipation::studentName))
                .toList();
    }
}
