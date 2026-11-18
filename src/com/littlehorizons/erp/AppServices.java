package com.littlehorizons.erp;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.ClubMembershipDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GradeLevelDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TeachingAssignmentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcAcademicRecordDao;
import com.littlehorizons.erp.dao.jdbc.JdbcAttendanceDao;
import com.littlehorizons.erp.dao.jdbc.JdbcChargeDao;
import com.littlehorizons.erp.dao.jdbc.JdbcClassScheduleDao;
import com.littlehorizons.erp.dao.jdbc.JdbcClubDao;
import com.littlehorizons.erp.dao.jdbc.JdbcClubMembershipDao;
import com.littlehorizons.erp.dao.jdbc.JdbcEmergencyContactDao;
import com.littlehorizons.erp.dao.jdbc.JdbcEnrollmentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcEventDao;
import com.littlehorizons.erp.dao.jdbc.JdbcEventParticipantDao;
import com.littlehorizons.erp.dao.jdbc.JdbcGradeLevelDao;
import com.littlehorizons.erp.dao.jdbc.JdbcGuardianDao;
import com.littlehorizons.erp.dao.jdbc.JdbcInvoiceDao;
import com.littlehorizons.erp.dao.jdbc.JdbcLoanDao;
import com.littlehorizons.erp.dao.jdbc.JdbcPaymentConceptDao;
import com.littlehorizons.erp.dao.jdbc.JdbcPaymentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcReportDao;
import com.littlehorizons.erp.dao.jdbc.JdbcResourceDao;
import com.littlehorizons.erp.dao.jdbc.JdbcSequenceDao;
import com.littlehorizons.erp.dao.jdbc.JdbcStudentAdjustmentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcStudentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcSubjectDao;
import com.littlehorizons.erp.dao.jdbc.JdbcSubjectEnrollmentDao;
import com.littlehorizons.erp.dao.jdbc.JdbcTeacherDao;
import com.littlehorizons.erp.dao.jdbc.JdbcTeachingAssignmentDao;
import com.littlehorizons.erp.service.AcademicService;
import com.littlehorizons.erp.service.AttendanceService;
import com.littlehorizons.erp.service.ExtracurricularService;
import com.littlehorizons.erp.service.FinanceService;
import com.littlehorizons.erp.service.ReportExportService;
import com.littlehorizons.erp.service.ReportService;
import com.littlehorizons.erp.service.ResourceService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.service.invoice.PdfInvoiceGenerator;
import com.littlehorizons.erp.service.invoice.TxtInvoiceGenerator;

import java.util.List;

/**
 * Composition root of the service layer: creates every DAO and injects them into the services
 * through their constructors (Dependency Injection without a framework). Used by {@link Main}
 * and by the smoke tests.
 */
public record AppServices(
        StudentService students,
        AcademicService academic,
        AttendanceService attendance,
        TeacherService teachers,
        ExtracurricularService extracurricular,
        FinanceService finance,
        ResourceService resources,
        ReportService reports,
        ReportExportService exports) {

    public static AppServices create(AppConfig config) {
        StudentDao studentDao = new JdbcStudentDao();
        GuardianDao guardianDao = new JdbcGuardianDao();
        EnrollmentDao enrollmentDao = new JdbcEnrollmentDao();
        GradeLevelDao gradeDao = new JdbcGradeLevelDao();
        SubjectDao subjectDao = new JdbcSubjectDao();
        SubjectEnrollmentDao subjectEnrollmentDao = new JdbcSubjectEnrollmentDao();
        TeacherDao teacherDao = new JdbcTeacherDao();
        TeachingAssignmentDao assignmentDao = new JdbcTeachingAssignmentDao();
        ClubMembershipDao membershipDao = new JdbcClubMembershipDao();
        PaymentDao paymentDao = new JdbcPaymentDao();
        ResourceDao resourceDao = new JdbcResourceDao();

        StudentService studentService = new StudentService(studentDao, guardianDao, new JdbcEmergencyContactDao(), enrollmentDao,
                gradeDao, subjectEnrollmentDao, membershipDao);
        AcademicService academicService = new AcademicService(subjectDao, subjectEnrollmentDao, new JdbcAcademicRecordDao(),
                enrollmentDao, studentDao, teacherDao, assignmentDao, gradeDao);
        AttendanceService attendanceService = new AttendanceService(new JdbcAttendanceDao(), studentDao, subjectDao,
                subjectEnrollmentDao, assignmentDao);
        TeacherService teacherService = new TeacherService(teacherDao, assignmentDao, new JdbcClassScheduleDao(), subjectDao);
        ExtracurricularService extracurricularService = new ExtracurricularService(new JdbcClubDao(), membershipDao, new JdbcEventDao(),
                new JdbcEventParticipantDao(), studentDao, teacherDao);
        FinanceService financeService = new FinanceService(new JdbcPaymentConceptDao(), new JdbcStudentAdjustmentDao(), new JdbcChargeDao(),
                paymentDao, new JdbcInvoiceDao(), new JdbcSequenceDao(), studentDao, guardianDao, enrollmentDao,
                List.of(new TxtInvoiceGenerator(config), new PdfInvoiceGenerator(config)), config);
        ResourceService resourceService = new ResourceService(resourceDao, new JdbcLoanDao(), studentDao, teacherDao);
        ReportService reportService = new ReportService(new JdbcReportDao(), paymentDao, resourceDao, financeService, resourceService);

        ReportExportService exportService = new ReportExportService(financeService, resourceService, attendanceService,
                academicService, extracurricularService, config.reportsOutputDir());

        return new AppServices(studentService, academicService, attendanceService, teacherService, extracurricularService,
                financeService, resourceService, reportService, exportService);
    }
}
