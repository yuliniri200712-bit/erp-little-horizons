package com.littlehorizons.erp;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.ChargeDao;
import com.littlehorizons.erp.dao.ClassScheduleDao;
import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.ClubMembershipDao;
import com.littlehorizons.erp.dao.EmergencyContactDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.EventParticipantDao;
import com.littlehorizons.erp.dao.GradeLevelDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.SequenceDao;
import com.littlehorizons.erp.dao.StudentAdjustmentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.TeachingAssignmentDao;
import com.littlehorizons.erp.service.AcademicService;
import com.littlehorizons.erp.service.AttendanceService;
import com.littlehorizons.erp.service.ExtracurricularService;
import com.littlehorizons.erp.service.FinanceService;
import com.littlehorizons.erp.service.ReportService;
import com.littlehorizons.erp.service.ResourceService;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.service.TeacherService;
import com.littlehorizons.erp.service.invoice.PdfInvoiceGenerator;

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
        ReportService reports) {

    public static AppServices create(AppConfig config) {
        StudentDao studentDao = new StudentDao();
        GuardianDao guardianDao = new GuardianDao();
        EnrollmentDao enrollmentDao = new EnrollmentDao();
        GradeLevelDao gradeDao = new GradeLevelDao();
        SubjectDao subjectDao = new SubjectDao();
        SubjectEnrollmentDao subjectEnrollmentDao = new SubjectEnrollmentDao();
        TeacherDao teacherDao = new TeacherDao();
        TeachingAssignmentDao assignmentDao = new TeachingAssignmentDao();
        ClubMembershipDao membershipDao = new ClubMembershipDao();
        PaymentDao paymentDao = new PaymentDao();
        ResourceDao resourceDao = new ResourceDao();

        StudentService studentService = new StudentService(studentDao, guardianDao, new EmergencyContactDao(), enrollmentDao,
                gradeDao, subjectEnrollmentDao, membershipDao);
        AcademicService academicService = new AcademicService(subjectDao, subjectEnrollmentDao, new AcademicRecordDao(),
                enrollmentDao, studentDao, teacherDao, assignmentDao, gradeDao);
        AttendanceService attendanceService = new AttendanceService(new AttendanceDao(), studentDao, subjectDao,
                subjectEnrollmentDao, assignmentDao);
        TeacherService teacherService = new TeacherService(teacherDao, assignmentDao, new ClassScheduleDao(), subjectDao);
        ExtracurricularService extracurricularService = new ExtracurricularService(new ClubDao(), membershipDao, new EventDao(),
                new EventParticipantDao(), studentDao, teacherDao);
        FinanceService financeService = new FinanceService(new PaymentConceptDao(), new StudentAdjustmentDao(), new ChargeDao(),
                paymentDao, new InvoiceDao(), new SequenceDao(), studentDao, guardianDao, enrollmentDao,
                new PdfInvoiceGenerator(config), config);
        ResourceService resourceService = new ResourceService(resourceDao, new LoanDao(), studentDao, teacherDao);
        ReportService reportService = new ReportService(new ReportDao(), paymentDao, resourceDao, financeService, resourceService);

        return new AppServices(studentService, academicService, attendanceService, teacherService, extracurricularService,
                financeService, resourceService, reportService);
    }
}
