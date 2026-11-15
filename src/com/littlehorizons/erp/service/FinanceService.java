package com.littlehorizons.erp.service;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.ChargeDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.SequenceDao;
import com.littlehorizons.erp.dao.StudentAdjustmentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Charge;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentAdjustment;
import com.littlehorizons.erp.model.enums.AdjustmentType;
import com.littlehorizons.erp.model.enums.ChargeStatus;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;
import com.littlehorizons.erp.model.enums.PaymentMethod;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.report.DebtRow;
import com.littlehorizons.erp.model.report.InvoiceData;
import com.littlehorizons.erp.service.invoice.InvoiceGenerator;
import com.littlehorizons.erp.util.CufeGenerator;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.util.Formatter;
import com.littlehorizons.erp.util.TransactionManager;
import com.littlehorizons.erp.util.Validator;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Billing concepts, scholarships/discounts, receivables, payments, invoices and arrears.
 * Confirming a payment stores it, issues the invoice (consecutive + simulated CUFE) in the same
 * transaction and then renders the PDF through {@link InvoiceGenerator}.
 */
public class FinanceService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal MAX_TAX_RATE = BigDecimal.valueOf(30);

    private final PaymentConceptDao conceptDao;
    private final StudentAdjustmentDao adjustmentDao;
    private final ChargeDao chargeDao;
    private final PaymentDao paymentDao;
    private final InvoiceDao invoiceDao;
    private final SequenceDao sequenceDao;
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final EnrollmentDao enrollmentDao;
    private final InvoiceGenerator invoiceGenerator;
    private final AppConfig config;

    public FinanceService(PaymentConceptDao conceptDao, StudentAdjustmentDao adjustmentDao, ChargeDao chargeDao,
                          PaymentDao paymentDao, InvoiceDao invoiceDao, SequenceDao sequenceDao, StudentDao studentDao,
                          GuardianDao guardianDao, EnrollmentDao enrollmentDao, InvoiceGenerator invoiceGenerator,
                          AppConfig config) {
        this.conceptDao = conceptDao;
        this.adjustmentDao = adjustmentDao;
        this.chargeDao = chargeDao;
        this.paymentDao = paymentDao;
        this.invoiceDao = invoiceDao;
        this.sequenceDao = sequenceDao;
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.enrollmentDao = enrollmentDao;
        this.invoiceGenerator = invoiceGenerator;
        this.config = config;
    }

    // ================================================================ concepts

    public List<PaymentConcept> listConcepts() {
        return conceptDao.findAll();
    }

    public PaymentConcept getConcept(int id) {
        return conceptDao.findById(id).orElseThrow(() -> new BusinessException("No existe el concepto con ID " + id + "."));
    }

    public PaymentConcept findConcept(String idOrCode) {
        String key = Validator.requireText(idOrCode, "ID o código del concepto", 20).toUpperCase();
        Optional<PaymentConcept> byCode = conceptDao.findByCode(key);
        if (byCode.isPresent()) {
            return byCode.get();
        }
        if (key.matches("\\d{1,9}")) {
            return getConcept(Integer.parseInt(key));
        }
        throw new BusinessException("No se encontró el concepto '" + key + "'.");
    }

    public int createConcept(PaymentConcept concept) {
        return conceptDao.insert(validateConcept(concept, null));
    }

    public void updateConcept(PaymentConcept concept) {
        getConcept(concept.id());
        conceptDao.update(validateConcept(concept, concept.id()));
    }

    public void deleteConcept(int conceptId) {
        getConcept(conceptId);
        conceptDao.delete(conceptId); // FK on pagos/cargos blocks deletion of used concepts
    }

    private PaymentConcept validateConcept(PaymentConcept c, Integer currentId) {
        String code = Validator.requireText(c.code(), "Código", 20).toUpperCase();
        if (!code.matches("^[A-Z0-9_-]{2,20}$")) {
            throw new BusinessException("El código solo admite letras mayúsculas, números, '-' y '_'.");
        }
        conceptDao.findByCode(code).filter(o -> !o.id().equals(currentId)).ifPresent(o -> {
            throw new BusinessException("Ya existe un concepto con código " + code + ".");
        });
        if (c.baseAmount() == null || c.baseAmount().signum() < 0) {
            throw new BusinessException("El valor base no puede ser negativo.");
        }
        BigDecimal tax = c.taxRate() == null ? BigDecimal.ZERO : c.taxRate();
        if (tax.signum() < 0 || tax.compareTo(MAX_TAX_RATE) > 0) {
            throw new BusinessException("El porcentaje de IVA debe estar entre 0 y 30.");
        }
        return new PaymentConcept(c.id(), code, Validator.requireText(c.name(), "Nombre", 100),
                Validator.optionalText(c.description(), "Descripción", 1000), c.baseAmount().setScale(2, RoundingMode.HALF_UP),
                tax.setScale(2, RoundingMode.HALF_UP), c.recurring(), c.active());
    }

    // ================================================================ scholarships and discounts

    public int addAdjustment(int studentId, int conceptId, AdjustmentType type, BigDecimal percentage, String reason) {
        requireStudent(studentId);
        getConcept(conceptId);
        if (type == null) {
            throw new BusinessException("Seleccione el tipo de ajuste (Beca o Descuento).");
        }
        if (percentage == null || percentage.signum() <= 0 || percentage.compareTo(HUNDRED) > 0) {
            throw new BusinessException("El porcentaje debe ser mayor que 0 y máximo 100.");
        }
        return adjustmentDao.insert(new StudentAdjustment(null, studentId, conceptId, null, type,
                percentage.setScale(2, RoundingMode.HALF_UP), Validator.requireText(reason, "Motivo", 255), true));
    }

    public List<StudentAdjustment> adjustmentsOf(int studentId) {
        return adjustmentDao.findByStudent(studentId);
    }

    public void toggleAdjustment(int adjustmentId) {
        StudentAdjustment adjustment = adjustmentDao.findById(adjustmentId)
                .orElseThrow(() -> new BusinessException("No existe el ajuste " + adjustmentId + "."));
        adjustmentDao.setActive(adjustmentId, !adjustment.active());
    }

    public void deleteAdjustment(int adjustmentId) {
        if (!adjustmentDao.delete(adjustmentId)) {
            throw new BusinessException("No existe el ajuste " + adjustmentId + ".");
        }
    }

    /**
     * Value to charge a student for a concept: base minus active scholarships/discounts (their
     * percentages are added and capped at 100 %) plus IVA, rounded to whole pesos.
     */
    public BigDecimal effectiveAmount(int studentId, int conceptId) {
        PaymentConcept concept = getConcept(conceptId);
        BigDecimal reduction = adjustmentDao.findActive(studentId, conceptId).stream()
                .map(StudentAdjustment::percentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .min(HUNDRED);
        BigDecimal net = concept.baseAmount().multiply(HUNDRED.subtract(reduction)).divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return net.multiply(HUNDRED.add(concept.taxRate())).divide(HUNDRED, 0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
    }

    // ================================================================ charges (receivables)

    public int createCharge(int studentId, int conceptId, String description, LocalDate issueDate, LocalDate dueDate) {
        Student student = requireStudent(studentId);
        if (student.status() != StudentStatus.ACTIVE) {
            throw new BusinessException("Solo se generan cobros a estudiantes activos.");
        }
        PaymentConcept concept = getConcept(conceptId);
        if (!concept.active()) {
            throw new BusinessException("El concepto " + concept.code() + " está inactivo.");
        }
        String text = Validator.requireText(description, "Descripción", 200);
        LocalDate issue = issueDate == null ? LocalDate.now() : issueDate;
        if (dueDate == null) {
            throw new BusinessException("La fecha de vencimiento es obligatoria.");
        }
        Validator.requireOrder(issue, dueDate, "Fecha de emisión", "Fecha de vencimiento");
        if (chargeDao.existsDuplicate(studentId, conceptId, text)) {
            throw new BusinessException("Ya existe un cobro '" + text + "' para este estudiante.");
        }
        BigDecimal amount = effectiveAmount(studentId, conceptId);
        if (amount.signum() <= 0) {
            throw new BusinessException("El estudiante tiene beca del 100 % para este concepto; no se genera cobro.");
        }
        return chargeDao.insert(new Charge(null, studentId, null, conceptId, null, text, amount, BigDecimal.ZERO, issue, dueDate,
                ChargeStatus.PENDING));
    }

    /** Creates the same charge for every student with an active enrollment in the year (optionally one grade). */
    public BulkChargeResult generateCharges(int conceptId, String description, LocalDate issueDate, LocalDate dueDate,
                                            int year, Integer gradeId) {
        PaymentConcept concept = getConcept(conceptId);
        if (!concept.active()) {
            throw new BusinessException("El concepto " + concept.code() + " está inactivo.");
        }
        String text = Validator.requireText(description, "Descripción", 200);
        Validator.requireOrder(issueDate, dueDate, "Fecha de emisión", "Fecha de vencimiento");
        List<Enrollment> enrollments = enrollmentDao.findByYear(year, gradeId).stream()
                .filter(e -> e.status() == EnrollmentStatus.ACTIVE)
                .toList();
        if (enrollments.isEmpty()) {
            throw new BusinessException("No hay estudiantes con matrícula activa para los criterios indicados.");
        }
        return TransactionManager.inTransaction(() -> {
            int created = 0;
            int duplicates = 0;
            int fullScholarship = 0;
            for (Enrollment enrollment : enrollments) {
                int studentId = enrollment.studentId();
                if (chargeDao.existsDuplicate(studentId, conceptId, text)) {
                    duplicates++;
                    continue;
                }
                BigDecimal amount = effectiveAmount(studentId, conceptId);
                if (amount.signum() <= 0) {
                    fullScholarship++;
                    continue;
                }
                chargeDao.insert(new Charge(null, studentId, null, conceptId, null, text, amount, BigDecimal.ZERO, issueDate,
                        dueDate, ChargeStatus.PENDING));
                created++;
            }
            return new BulkChargeResult(created, duplicates, fullScholarship);
        });
    }

    public Charge getCharge(int chargeId) {
        return chargeDao.findById(chargeId).orElseThrow(() -> new BusinessException("No existe el cobro " + chargeId + "."));
    }

    public List<Charge> chargesOf(int studentId, boolean pendingOnly) {
        return chargeDao.findByStudent(studentId, pendingOnly);
    }

    public void cancelCharge(int chargeId) {
        Charge charge = getCharge(chargeId);
        if (charge.status() != ChargeStatus.PENDING) {
            throw new BusinessException("Solo se anulan cobros pendientes.");
        }
        if (charge.paidAmount().signum() > 0) {
            throw new BusinessException("El cobro tiene abonos registrados; no puede anularse.");
        }
        chargeDao.updateStatus(chargeId, ChargeStatus.CANCELLED);
    }

    // ================================================================ payments

    /**
     * Registers a payment and issues its invoice. Validations: amount &gt; 0, date not in the
     * future, paying guardian linked to the student, charge (if any) pending, of the same student
     * and with enough balance. The PDF is generated after the commit; if it fails the payment is
     * kept and the invoice can be regenerated later.
     */
    public PaymentResult registerPayment(PaymentRequest request) {
        Student student = requireStudent(request.studentId());
        BigDecimal amount = Validator.requirePositive(request.amount(), "Monto").setScale(2, RoundingMode.HALF_UP);
        LocalDate date = request.paymentDate() == null ? LocalDate.now() : request.paymentDate();
        Validator.requireNotFuture(date, "Fecha de pago");
        if (request.method() == null) {
            throw new BusinessException("Seleccione el método de pago.");
        }
        Guardian payer = resolvePayer(student, request.guardianId());

        Payment saved = TransactionManager.inTransaction(() -> {
            int conceptId = request.conceptId();
            Integer chargeId = request.chargeId();
            if (chargeId != null) {
                chargeDao.lockForUpdate(chargeId);
                Charge charge = getCharge(chargeId);
                validateChargeForPayment(charge, student, amount);
                conceptId = charge.conceptId();
            }
            PaymentConcept concept = getConcept(conceptId);
            if (!concept.active()) {
                throw new BusinessException("El concepto " + concept.code() + " está inactivo.");
            }
            String receipt = sequenceDao.next(SequenceDao.RECEIPT);
            int paymentId = paymentDao.insert(new Payment(null, student.id(), null, payer.id(), null, conceptId, null, chargeId,
                    amount, date, request.method(), receipt, date.getYear(), request.period(),
                    Validator.optionalText(request.notes(), "Notas", 1000)));
            if (chargeId != null) {
                Charge charge = getCharge(chargeId);
                if (charge.pending().signum() == 0) {
                    chargeDao.updateStatus(chargeId, ChargeStatus.PAID);
                }
            }
            Payment payment = getPayment(paymentId);
            issueInvoice(payment, concept);
            return payment;
        });

        Invoice invoice = invoiceDao.findByPayment(saved.id()).orElseThrow();
        try {
            Path pdf = renderPdf(saved, invoice);
            return new PaymentResult(saved, invoiceDao.findByPayment(saved.id()).orElse(invoice), pdf, null);
        } catch (IOException | RuntimeException e) {
            ErrorLogger.error("Invoice PDF " + invoice.number(), e);
            return new PaymentResult(saved, invoice, null, e.getMessage());
        }
    }

    private Guardian resolvePayer(Student student, Integer guardianId) {
        if (guardianId == null) {
            return guardianDao.findFinancialGuardian(student.id())
                    .orElseThrow(() -> new BusinessException("El estudiante no tiene acudiente responsable registrado."));
        }
        Guardian guardian = guardianDao.findById(guardianId)
                .orElseThrow(() -> new BusinessException("No existe el acudiente " + guardianId + "."));
        if (!guardianDao.isLinked(student.id(), guardianId)) {
            throw new BusinessException(guardian.fullName() + " no es acudiente de " + student.fullName() + ".");
        }
        return guardian;
    }

    private static void validateChargeForPayment(Charge charge, Student student, BigDecimal amount) {
        if (charge.studentId() != student.id()) {
            throw new BusinessException("El cobro " + charge.id() + " no pertenece a " + student.fullName() + ".");
        }
        if (charge.status() != ChargeStatus.PENDING) {
            throw new BusinessException("El cobro " + charge.id() + " no está pendiente (estado: " + charge.status() + ").");
        }
        if (amount.compareTo(charge.pending()) > 0) {
            throw new BusinessException("El monto supera el saldo pendiente del cobro (" + Formatter.money(charge.pending()) + ").");
        }
    }

    /** Creates the invoice record: next consecutive, totals with IVA if applicable and simulated CUFE. */
    private Invoice issueInvoice(Payment payment, PaymentConcept concept) {
        String number = sequenceDao.next(SequenceDao.INVOICE);
        BigDecimal total = payment.amount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = total.multiply(HUNDRED).divide(HUNDRED.add(concept.taxRate()), 2, RoundingMode.HALF_UP);
        BigDecimal tax = total.subtract(subtotal);
        LocalDate issueDate = LocalDate.now();
        String cufe = CufeGenerator.simulatedCufe(number, config.companyNit(), issueDate, total);
        Invoice invoice = new Invoice(null, number, payment.id(), issueDate, subtotal, tax, total, cufe, null, null);
        int id = invoiceDao.insert(invoice);
        return new Invoice(id, number, payment.id(), issueDate, subtotal, tax, total, cufe, null, null);
    }

    /** Generates (or regenerates) the PDF of a payment's invoice, issuing the invoice if missing. */
    public Path generateInvoicePdf(int paymentId) throws IOException {
        Payment payment = getPayment(paymentId);
        Invoice invoice = invoiceDao.findByPayment(paymentId).orElseGet(() ->
                TransactionManager.inTransaction(() -> issueInvoice(payment, getConcept(payment.conceptId()))));
        return renderPdf(payment, invoice);
    }

    private Path renderPdf(Payment payment, Invoice invoice) throws IOException {
        Student student = requireStudent(payment.studentId());
        Guardian guardian = payment.guardianId() == null ? guardianDao.findFinancialGuardian(student.id()).orElse(null)
                : guardianDao.findById(payment.guardianId()).orElse(null);
        PaymentConcept concept = getConcept(payment.conceptId());
        String gradeName = enrollmentDao.findByStudentAndYear(student.id(), payment.paymentDate().getYear())
                .map(Enrollment::gradeName).orElse("-");
        String line = concept.name() + (payment.chargeId() == null ? ""
                : " - " + chargeDao.findById(payment.chargeId()).map(Charge::description).orElse(""))
                + (payment.notes() == null ? "" : " (" + payment.notes() + ")");
        Path file = invoiceGenerator.generate(new InvoiceData(invoice, payment, student, guardian, concept, gradeName, line));
        invoiceDao.updateFile(invoice.id(), file.getFileName().toString(), file.toAbsolutePath().toString());
        return file;
    }

    public Payment getPayment(int paymentId) {
        return paymentDao.findById(paymentId).orElseThrow(() -> new BusinessException("No existe el pago " + paymentId + "."));
    }

    public Payment findPayment(String idOrReceipt) {
        String key = Validator.requireText(idOrReceipt, "ID o recibo", 50).toUpperCase();
        Optional<Payment> byReceipt = paymentDao.findByReceipt(key);
        if (byReceipt.isPresent()) {
            return byReceipt.get();
        }
        if (key.matches("\\d{1,9}")) {
            return getPayment(Integer.parseInt(key));
        }
        throw new BusinessException("No se encontró el pago '" + key + "'.");
    }

    public List<Payment> paymentsOfStudent(int studentId) {
        requireStudent(studentId);
        return paymentDao.findByStudent(studentId);
    }

    public List<Payment> paymentsOfGuardian(int guardianId) {
        guardianDao.findById(guardianId).orElseThrow(() -> new BusinessException("No existe el acudiente " + guardianId + "."));
        return paymentDao.findByGuardian(guardianId);
    }

    public List<Payment> recentPayments(int limit) {
        return paymentDao.findRecent(limit);
    }

    public BigDecimal total(List<Payment> payments) {
        return payments.stream().map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void updatePayment(int paymentId, PaymentMethod method, String notes) {
        Payment payment = getPayment(paymentId);
        paymentDao.updateDetails(paymentId, method == null ? payment.method() : method,
                Validator.optionalText(notes, "Notas", 1000));
    }

    /** Deletes (annuls) a payment; its invoice is removed by cascade and the charge reopens. */
    public void deletePayment(int paymentId) {
        Payment payment = getPayment(paymentId);
        TransactionManager.inTransaction(() -> {
            paymentDao.delete(paymentId);
            if (payment.chargeId() != null) {
                Charge charge = getCharge(payment.chargeId());
                if (charge.status() == ChargeStatus.PAID && charge.pending().signum() > 0) {
                    chargeDao.updateStatus(charge.id(), ChargeStatus.PENDING);
                }
            }
        });
    }

    public List<Invoice> listInvoices() {
        return invoiceDao.findAll();
    }

    public Optional<Invoice> invoiceOf(int paymentId) {
        return invoiceDao.findByPayment(paymentId);
    }

    // ================================================================ arrears

    /** Pending charges overdue by more than one month (due date before today minus one month). */
    public List<DebtRow> debtsOverOneMonth() {
        return chargeDao.overdueBefore(LocalDate.now().minusMonths(1));
    }

    /** Every pending charge already past its due date. */
    public List<DebtRow> overdueDebts() {
        return chargeDao.overdueBefore(LocalDate.now());
    }

    public BigDecimal totalPending(List<DebtRow> debts) {
        return debts.stream().map(DebtRow::pending).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Student requireStudent(int studentId) {
        return studentDao.findById(studentId)
                .orElseThrow(() -> new BusinessException("No existe un estudiante con ID " + studentId + "."));
    }
}
