package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.ClubMembershipDao;
import com.littlehorizons.erp.dao.EmergencyContactDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GradeLevelDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.EmergencyContact;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.GradeLevel;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.GuardianLink;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.util.TransactionManager;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Use cases of students, guardians, emergency contacts and yearly enrollment (matrícula).
 * Enforces: unique documents, at least one guardian per student, coherent grade progression
 * and consistent status changes (withdrawal / graduation).
 */
public class StudentService {

    private static final int MIN_AGE = 3;
    private static final int MAX_AGE = 25;
    private static final int MAX_GRADE_LEVEL = 11;

    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final EmergencyContactDao contactDao;
    private final EnrollmentDao enrollmentDao;
    private final GradeLevelDao gradeDao;
    private final SubjectEnrollmentDao subjectEnrollmentDao;
    private final ClubMembershipDao membershipDao;

    public StudentService(StudentDao studentDao, GuardianDao guardianDao, EmergencyContactDao contactDao,
                          EnrollmentDao enrollmentDao, GradeLevelDao gradeDao, SubjectEnrollmentDao subjectEnrollmentDao,
                          ClubMembershipDao membershipDao) {
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.contactDao = contactDao;
        this.enrollmentDao = enrollmentDao;
        this.gradeDao = gradeDao;
        this.subjectEnrollmentDao = subjectEnrollmentDao;
        this.membershipDao = membershipDao;
    }

    // ================================================================ students

    public List<Student> listStudents() {
        return studentDao.findAll();
    }

    public List<Student> listStudents(StudentStatus status) {
        return studentDao.findByStatus(status);
    }

    public List<Student> searchStudents(String text) {
        return studentDao.search(Validator.requireText(text, "Texto de búsqueda", 100));
    }

    public Student getStudent(int id) {
        return studentDao.findById(id).orElseThrow(() -> new BusinessException("No existe un estudiante con ID " + id + "."));
    }

    /** Looks up a student by document number first and then by numeric ID. */
    public Student findStudent(String idOrDocument) {
        String key = Validator.requireText(idOrDocument, "ID o documento", 20);
        Optional<Student> byDocument = studentDao.findByDocument(key);
        if (byDocument.isPresent()) {
            return byDocument.get();
        }
        if (key.matches("\\d{1,9}")) {
            return getStudent(Integer.parseInt(key));
        }
        throw new BusinessException("No se encontró un estudiante con ID o documento '" + key + "'.");
    }

    /**
     * Registers a student and links its first guardian in one transaction. Either an existing
     * guardian ({@code existingGuardianId}) or a new one ({@code newGuardian}) must be given.
     */
    public int registerStudent(Student student, Integer existingGuardianId, Guardian newGuardian) {
        Student valid = validateStudent(student, null);
        if (existingGuardianId == null && newGuardian == null) {
            throw new BusinessException("Todo estudiante debe tener al menos un acudiente.");
        }
        Guardian validGuardian = newGuardian == null ? null : validateGuardian(newGuardian, null);
        return TransactionManager.inTransaction(() -> {
            int studentId = studentDao.insert(valid);
            int guardianId = existingGuardianId != null ? getGuardian(existingGuardianId).id() : guardianDao.insert(validGuardian);
            guardianDao.link(studentId, guardianId, true, true);
            return studentId;
        });
    }

    public void updateStudent(Student student) {
        getStudent(student.id());
        studentDao.update(validateStudent(student, student.id()));
    }

    public void deleteStudent(int studentId) {
        getStudent(studentId);
        if (studentDao.countPayments(studentId) > 0) {
            throw new BusinessException("El estudiante tiene pagos registrados; no puede eliminarse. Cambie su estado a 'Retirado'.");
        }
        studentDao.delete(studentId);
    }

    /**
     * Changes the student status keeping enrollments consistent: withdrawal closes the current
     * enrollment, subject enrollments and clubs; graduation requires the last enrollment in grade 11.
     */
    public void changeStatus(int studentId, StudentStatus newStatus, LocalDate date, String reason) {
        Student student = getStudent(studentId);
        LocalDate effectiveDate = date == null ? LocalDate.now() : date;
        Validator.requireNotFuture(effectiveDate, "Fecha del cambio");
        if (student.status() == newStatus) {
            throw new BusinessException("El estudiante ya está en estado '" + newStatus + "'.");
        }
        if (student.status() == StudentStatus.GRADUATED) {
            throw new BusinessException("Un estudiante graduado no puede cambiar de estado.");
        }
        TransactionManager.inTransaction(() -> {
            switch (newStatus) {
                case GRADUATED -> graduate(student, effectiveDate);
                case WITHDRAWN -> withdraw(student, effectiveDate, reason);
                case SUSPENDED -> {
                    if (student.status() != StudentStatus.ACTIVE) {
                        throw new BusinessException("Solo un estudiante activo puede ser suspendido.");
                    }
                }
                case ACTIVE -> { /* reactivation from Retirado or Suspendido */ }
            }
            studentDao.updateStatus(studentId, newStatus);
        });
    }

    private void graduate(Student student, LocalDate date) {
        Enrollment last = enrollmentDao.findLatest(student.id())
                .orElseThrow(() -> new BusinessException("El estudiante no tiene matrículas registradas."));
        if (last.gradeLevel() != MAX_GRADE_LEVEL) {
            throw new BusinessException("Solo puede graduarse un estudiante cuya última matrícula sea de grado Once.");
        }
        if (last.status() != EnrollmentStatus.WITHDRAWN) {
            enrollmentDao.updateStatus(last.id(), EnrollmentStatus.COMPLETED, null, "Graduado el " + date);
        }
        membershipDao.withdrawAllOfStudent(student.id(), date, "Graduación");
    }

    private void withdraw(Student student, LocalDate date, String reason) {
        String note = reason == null || reason.isBlank() ? "Retiro del estudiante" : reason.trim();
        enrollmentDao.findByStudentAndYear(student.id(), date.getYear())
                .filter(e -> e.status() == EnrollmentStatus.ACTIVE)
                .ifPresent(e -> {
                    Validator.requireOrder(e.enrollmentDate(), date, "Fecha de matrícula", "Fecha de retiro");
                    enrollmentDao.updateStatus(e.id(), EnrollmentStatus.WITHDRAWN, date, note);
                });
        subjectEnrollmentDao.withdrawAllActive(student.id(), date.getYear());
        membershipDao.withdrawAllOfStudent(student.id(), date, note);
    }

    private Student validateStudent(Student s, Integer currentId) {
        String document = Validator.requireDocument(s.documentNumber());
        studentDao.findByDocument(document)
                .filter(other -> !other.id().equals(currentId))
                .ifPresent(other -> {
                    throw new BusinessException("Ya existe un estudiante con el documento " + document + ".");
                });
        if (s.birthDate() == null || s.admissionDate() == null) {
            throw new BusinessException("Las fechas de nacimiento e ingreso son obligatorias.");
        }
        Validator.requireNotFuture(s.birthDate(), "Fecha de nacimiento");
        int age = Period.between(s.birthDate(), LocalDate.now()).getYears();
        Validator.requireRange(age, MIN_AGE, MAX_AGE, "Edad del estudiante");
        Validator.requireOrder(s.birthDate(), s.admissionDate(), "Fecha de nacimiento", "Fecha de ingreso");
        if (s.admissionDate().isAfter(LocalDate.now().plusYears(1))) {
            throw new BusinessException("La fecha de ingreso no puede ser posterior a un año desde hoy.");
        }
        if (s.documentType() == null || s.gender() == null) {
            throw new BusinessException("Tipo de documento y género son obligatorios.");
        }
        return new Student(s.id(), s.documentType(), document,
                Validator.requireText(s.firstName(), "Primer nombre", 100),
                Validator.optionalText(s.middleName(), "Segundo nombre", 100),
                Validator.requireText(s.lastName(), "Primer apellido", 100),
                Validator.optionalText(s.secondLastName(), "Segundo apellido", 100),
                s.birthDate(), s.gender(),
                Validator.optionalText(s.address(), "Dirección", 255),
                Validator.optionalPhone(s.phone()),
                Validator.optionalEmail(s.email()),
                Validator.optionalText(s.bloodType(), "Tipo de sangre", 5),
                Validator.optionalText(s.allergies(), "Alergias", 1000),
                Validator.optionalText(s.medicalConditions(), "Condiciones médicas", 1000),
                s.admissionDate(), s.status() == null ? StudentStatus.ACTIVE : s.status());
    }

    // ================================================================ guardians

    public List<Guardian> listGuardians() {
        return guardianDao.findAll();
    }

    public List<Guardian> searchGuardians(String text) {
        return guardianDao.search(Validator.requireText(text, "Texto de búsqueda", 100));
    }

    public Guardian getGuardian(int id) {
        return guardianDao.findById(id).orElseThrow(() -> new BusinessException("No existe un acudiente con ID " + id + "."));
    }

    public Guardian findGuardian(String idOrDocument) {
        String key = Validator.requireText(idOrDocument, "ID o documento", 20);
        Optional<Guardian> byDocument = guardianDao.findByDocument(key);
        if (byDocument.isPresent()) {
            return byDocument.get();
        }
        if (key.matches("\\d{1,9}")) {
            return getGuardian(Integer.parseInt(key));
        }
        throw new BusinessException("No se encontró un acudiente con ID o documento '" + key + "'.");
    }

    public int createGuardian(Guardian guardian) {
        return guardianDao.insert(validateGuardian(guardian, null));
    }

    public void updateGuardian(Guardian guardian) {
        getGuardian(guardian.id());
        guardianDao.update(validateGuardian(guardian, guardian.id()));
    }

    public void deleteGuardian(int guardianId) {
        getGuardian(guardianId);
        if (guardianDao.countStudentsWithOnlyThisGuardian(guardianId) > 0) {
            throw new BusinessException("Es el único acudiente de al menos un estudiante; vincule otro acudiente antes de eliminarlo.");
        }
        if (guardianDao.countPayments(guardianId) > 0) {
            throw new BusinessException("El acudiente tiene pagos registrados a su nombre; no puede eliminarse.");
        }
        guardianDao.delete(guardianId);
    }

    public void linkGuardian(int studentId, int guardianId, boolean primary, boolean emergencyContact) {
        getStudent(studentId);
        getGuardian(guardianId);
        if (guardianDao.isLinked(studentId, guardianId)) {
            throw new BusinessException("El acudiente ya está vinculado a este estudiante.");
        }
        TransactionManager.inTransaction(() -> {
            if (primary) {
                guardianDao.clearPrimary(studentId);
            }
            guardianDao.link(studentId, guardianId, primary, emergencyContact);
        });
    }

    public void unlinkGuardian(int studentId, int guardianId) {
        if (!guardianDao.isLinked(studentId, guardianId)) {
            throw new BusinessException("El acudiente no está vinculado a este estudiante.");
        }
        if (guardianDao.countGuardiansOf(studentId) <= 1) {
            throw new BusinessException("No se puede desvincular: el estudiante debe conservar al menos un acudiente.");
        }
        guardianDao.unlink(studentId, guardianId);
    }

    public List<GuardianLink> guardiansOf(int studentId) {
        return guardianDao.findByStudent(studentId);
    }

    public List<Student> studentsOf(int guardianId) {
        return guardianDao.findStudentsOf(guardianId);
    }

    private Guardian validateGuardian(Guardian g, Integer currentId) {
        String document = Validator.requireDocument(g.documentNumber());
        guardianDao.findByDocument(document)
                .filter(other -> !other.id().equals(currentId))
                .ifPresent(other -> {
                    throw new BusinessException("Ya existe un acudiente con el documento " + document + ".");
                });
        if (g.documentType() == null || !Arrays.asList(DocumentType.adultTypes()).contains(g.documentType())) {
            throw new BusinessException("El acudiente debe identificarse con CC, CE o Pasaporte.");
        }
        return new Guardian(g.id(), g.documentType(), document,
                Validator.requireText(g.firstName(), "Primer nombre", 100),
                Validator.optionalText(g.middleName(), "Segundo nombre", 100),
                Validator.requireText(g.lastName(), "Primer apellido", 100),
                Validator.optionalText(g.secondLastName(), "Segundo apellido", 100),
                Validator.requireText(g.relationship(), "Parentesco", 50),
                Validator.requirePhone(g.mainPhone()),
                Validator.optionalPhone(g.secondaryPhone()),
                Validator.optionalEmail(g.email()),
                Validator.optionalText(g.address(), "Dirección", 255),
                Validator.optionalText(g.occupation(), "Ocupación", 100),
                g.financialResponsible());
    }

    // ================================================================ emergency contacts

    public int addEmergencyContact(EmergencyContact contact) {
        getStudent(contact.studentId());
        return contactDao.insert(new EmergencyContact(null, contact.studentId(),
                Validator.requireText(contact.fullName(), "Nombre completo", 200),
                Validator.requireText(contact.relationship(), "Parentesco", 50),
                Validator.requirePhone(contact.phone()),
                Validator.optionalPhone(contact.alternatePhone()),
                Validator.optionalText(contact.notes(), "Observaciones", 500)));
    }

    public List<EmergencyContact> emergencyContactsOf(int studentId) {
        return contactDao.findByStudent(studentId);
    }

    public void deleteEmergencyContact(int studentId, int contactId) {
        EmergencyContact contact = contactDao.findById(contactId)
                .filter(c -> c.studentId() == studentId)
                .orElseThrow(() -> new BusinessException("El contacto " + contactId + " no pertenece al estudiante."));
        contactDao.delete(contact.id());
    }

    // ================================================================ enrollment (matrícula)

    public List<GradeLevel> grades() {
        return gradeDao.findAll();
    }

    public GradeLevel getGrade(int gradeId) {
        return gradeDao.findById(gradeId).orElseThrow(() -> new BusinessException("No existe el grado con ID " + gradeId + "."));
    }

    /**
     * Enrolls an active student in a grade for an academic year. The new grade cannot be lower
     * than the previous year's grade nor skip more than one level.
     */
    public int enroll(int studentId, int gradeId, int year, LocalDate date, String notes) {
        Student student = getStudent(studentId);
        GradeLevel grade = getGrade(gradeId);
        int currentYear = LocalDate.now().getYear();
        Validator.requireRange(year, currentYear - 1, currentYear + 1, "Año académico");
        if (student.status() != StudentStatus.ACTIVE) {
            throw new BusinessException("Solo se matriculan estudiantes en estado Activo (estado actual: " + student.status() + ").");
        }
        if (enrollmentDao.findByStudentAndYear(studentId, year).isPresent()) {
            throw new BusinessException("El estudiante ya tiene matrícula para el año " + year + ".");
        }
        enrollmentDao.findByStudentAndYear(studentId, year - 1).ifPresent(previous -> {
            if (grade.level() < previous.gradeLevel()) {
                throw new BusinessException("El grado no puede ser inferior al del año anterior (" + previous.gradeName() + ").");
            }
            if (grade.level() > previous.gradeLevel() + 1) {
                throw new BusinessException("No se puede saltar grados: el año anterior cursó " + previous.gradeName() + ".");
            }
        });
        LocalDate enrollmentDate = date == null ? LocalDate.now() : date;
        Validator.requireNotFuture(enrollmentDate, "Fecha de matrícula");
        return enrollmentDao.insert(new Enrollment(null, studentId, gradeId, grade.name(), grade.level(), year,
                EnrollmentStatus.ACTIVE, enrollmentDate, null, Validator.optionalText(notes, "Observaciones", 500)));
    }

    public List<Enrollment> enrollmentsOf(int studentId) {
        return enrollmentDao.findByStudent(studentId);
    }

    public List<Enrollment> enrollmentsByYear(int year, Integer gradeId) {
        return enrollmentDao.findByYear(year, gradeId);
    }

    public Optional<Enrollment> enrollmentOf(int studentId, int year) {
        return enrollmentDao.findByStudentAndYear(studentId, year);
    }

    public List<Student> enrolledStudents(int gradeId, int year) {
        return studentDao.findEnrolled(gradeId, year);
    }
}
