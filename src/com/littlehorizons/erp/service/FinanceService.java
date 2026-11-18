package com.littlehorizons.erp.service;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.config.BillingProfile;
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
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.Charge;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoiceLine;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentAdjustment;
import com.littlehorizons.erp.model.dto.BulkChargeResult;
import com.littlehorizons.erp.model.dto.InvoiceDocument;
import com.littlehorizons.erp.model.dto.PaymentRequest;
import com.littlehorizons.erp.model.dto.PaymentResult;
import com.littlehorizons.erp.model.enums.AdjustmentType;
import com.littlehorizons.erp.model.enums.ChargeStatus;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;
import com.littlehorizons.erp.model.enums.PaymentMethod;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.report.DebtRow;
import com.littlehorizons.erp.model.report.InvoiceData;
import com.littlehorizons.erp.service.invoice.InvoiceGenerator;
import com.littlehorizons.erp.service.invoice.InvoiceTextRenderer;
import com.littlehorizons.erp.util.CufeGenerator;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.util.Formatter;
import com.littlehorizons.erp.util.TransactionManager;
import com.littlehorizons.erp.util.Validator;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Billing concepts, scholarships/discounts, receivables, payments, electronic invoices and arrears.
 * Confirming a payment stores it and then issues its invoice (consecutive, simulated CUFE and
 * resolution); invoices can also group several payments. Invoices are saved through the
 * {@link InvoiceGenerator} strategies (TXT and PDF).
 */
public class FinanceService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal MAX_TAX_RATE = BigDecimal.valueOf(30);
    private static final ZoneId COLOMBIA = ZoneId.of("America/Bogota");

    private final PaymentConceptDao conceptDao;
    private final StudentAdjustmentDao adjustmentDao;
    private final ChargeDao chargeDao;
    private final PaymentDao paymentDao;
    private final InvoiceDao invoiceDao;
    private final SequenceDao sequenceDao;
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final EnrollmentDao enrollmentDao;
    private final List<InvoiceGenerator> invoiceGenerators;
    private final AppConfig config;

    public FinanceService(PaymentConceptDao conceptDao, StudentAdjustmentDao adjustmentDao, ChargeDao chargeDao,
                          PaymentDao paymentDao, InvoiceDao invoiceDao, SequenceDao sequenceDao, StudentDao studentDao,
                          GuardianDao guardianDao, EnrollmentDao enrollmentDao, List<InvoiceGenerator> invoiceGenerators,
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
        this.invoiceGenerators = List.copyOf(invoiceGenerators);
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
     * Registers a payment and then issues its electronic invoice to the guardian who paid. Validations:
     * amount &gt; 0, date not in the future, paying guardian linked to the student, charge (if any)
     * pending, of the same student and with enough balance. The payment is committed first: if the
     * invoice cannot be issued (expired resolution, numbering range used up...) the payment is kept and
     * can be invoiced later from the invoices menu.
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
            if (chargeId != null && getCharge(chargeId).pending().signum() == 0) {
                chargeDao.updateStatus(chargeId, ChargeStatus.PAID);
            }
            return getPayment(paymentId);
        });

        try {
            return new PaymentResult(saved, issueInvoice(student.id(), List.of(saved.id()), payer.id()), null);
        } catch (BusinessException | DataAccessException e) {
            ErrorLogger.error("Invoice for payment " + saved.receiptNumber(), e);
            return new PaymentResult(saved, null, e.getMessage());
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

    /**
     * Deletes (annuls) a payment that is not invoiced yet; the charge it covered reopens. An invoiced
     * payment cannot be deleted: an issued electronic invoice is never modified (in real life it is
     * cancelled with a credit note).
     */
    public void deletePayment(int paymentId) {
        Payment payment = getPayment(paymentId);
        invoiceDao.findByPayment(paymentId).ifPresent(invoice -> {
            throw new BusinessException("El pago " + payment.receiptNumber() + " está incluido en la factura " + invoice.number()
                    + ": una factura emitida no se modifica ni se elimina (se anularía con una nota crédito).");
        });
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

    // ================================================================ electronic invoices (simulated)

    /** Payments of the student that are not in any invoice yet, oldest first. */
    public List<Payment> unbilledPayments(int studentId) {
        requireStudent(studentId);
        return paymentDao.findUnbilledByStudent(studentId);
    }

    /**
     * Who the invoice is issued to by default: the guardian who paid, when every chosen payment was paid
     * by the same one; otherwise the financially responsible guardian (the primary one first).
     */
    public Guardian suggestedClient(int studentId, List<Payment> payments) {
        Student student = requireStudent(studentId);
        List<Integer> payers = payments.stream().map(Payment::guardianId).distinct().toList();
        if (payers.size() == 1 && payers.getFirst() != null && guardianDao.isLinked(studentId, payers.getFirst())) {
            return guardianDao.findById(payers.getFirst()).orElseThrow();
        }
        return guardianDao.findFinancialGuardian(studentId)
                .orElseThrow(() -> new BusinessException(student.fullName() + " no tiene acudientes registrados."));
    }

    /**
     * Issues ONE electronic invoice with the chosen payments of the student, to the chosen guardian, and
     * saves it as TXT and PDF. Rules: 1 to {@value Invoice#MAX_LINES} payments, no repeated payment, only
     * payments of this student that are not invoiced yet, client linked to the student and with an
     * address, resolution in force and number inside its authorized range. The consecutive is locked
     * (SELECT ... FOR UPDATE) so two invoices never get the same number nor the same payment.
     */
    public InvoiceDocument issueInvoice(int studentId, List<Integer> paymentIds, int clientId) {
        Student student = requireStudent(studentId);
        if (paymentIds == null || paymentIds.isEmpty()) {
            throw new BusinessException("Elija al menos un pago para facturar.");
        }
        if (paymentIds.size() > Invoice.MAX_LINES) {
            throw new BusinessException("Una factura admite máximo " + Invoice.MAX_LINES + " pagos.");
        }
        if (Set.copyOf(paymentIds).size() != paymentIds.size()) {
            throw new BusinessException("Un pago no puede aparecer dos veces en la misma factura.");
        }
        BillingProfile profile = BillingProfile.from(config);
        LocalDateTime now = LocalDateTime.now(COLOMBIA).truncatedTo(ChronoUnit.SECONDS);
        profile.requireValidOn(now.toLocalDate());
        Guardian client = requireBillableClient(student, clientId);

        String issuedNumber = TransactionManager.inTransaction(() -> {
            String number = sequenceDao.next(SequenceDao.INVOICE); // locks the counter until commit
            profile.requireInRange(number);
            Map<Integer, Payment> available = paymentDao.findUnbilledByStudent(studentId).stream()
                    .collect(Collectors.toMap(Payment::id, Function.identity()));
            List<InvoiceLine> lines = paymentIds.stream().map(id -> {
                Payment payment = available.get(id);
                if (payment == null) {
                    String receipt = paymentDao.findById(id).map(Payment::receiptNumber).orElse("con ID " + id);
                    throw new BusinessException("El pago " + receipt + " no es de " + student.fullName() + " o ya fue facturado.");
                }
                return lineOf(payment);
            }).toList();
            BigDecimal subtotal = sum(lines, InvoiceLine::subtotal);
            BigDecimal tax = sum(lines, InvoiceLine::taxAmount);
            BigDecimal total = subtotal.add(tax);
            String cufe = CufeGenerator.simulatedCufe(number, now, subtotal, tax, total, profile.nitDigits(),
                    client.documentNumber(), profile.technicalKey(), BillingProfile.ENVIRONMENT);
            int id = invoiceDao.insert(new Invoice(null, number, studentId, null, clientId, null, now, subtotal, tax, total,
                    cufe, profile.resolutionText(), null, null, lines.size()));
            lines.forEach(line -> invoiceDao.insertLine(line.withInvoiceId(id)));
            return number;
        });
        return saveFiles(requireInvoice(issuedNumber), true);
    }

    /** The invoice with that number ("13" or "FE-000013") and its text; the files are not touched. */
    public InvoiceDocument viewInvoice(String number) {
        Invoice invoice = requireInvoice(number);
        InvoiceData data = invoiceData(invoice);
        List<Path> files = invoiceGenerators.stream().map(g -> g.pathFor(invoice.number())).filter(Files::exists).toList();
        return new InvoiceDocument(data, InvoiceTextRenderer.render(data), files, null);
    }

    /** Writes the TXT and PDF files of an existing invoice again, with the same content (built from saved data). */
    public InvoiceDocument regenerateInvoiceFiles(String number) {
        return saveFiles(requireInvoice(number), false);
    }

    public List<Invoice> invoicesOf(int studentId) {
        requireStudent(studentId);
        return invoiceDao.findByStudent(studentId);
    }

    public List<Invoice> listInvoices() {
        return invoiceDao.findAll();
    }

    public Optional<Invoice> invoiceOf(int paymentId) {
        return invoiceDao.findByPayment(paymentId);
    }

    private Invoice requireInvoice(String number) {
        String normalized = Invoice.normalizeNumber(number);
        return invoiceDao.findByNumber(normalized)
                .orElseThrow(() -> new BusinessException("No existe la factura " + normalized + "."));
    }

    private Guardian requireBillableClient(Student student, int clientId) {
        Guardian client = guardianDao.findById(clientId)
                .orElseThrow(() -> new BusinessException("No existe el acudiente " + clientId + "."));
        if (!guardianDao.isLinked(student.id(), clientId)) {
            throw new BusinessException(client.fullName() + " no es acudiente de " + student.fullName() + ".");
        }
        if (client.address() == null || client.address().isBlank()) {
            throw new BusinessException(client.fullName() + " no tiene dirección registrada y la factura la exige. "
                    + "Actualícela en Gestión de Estudiantes > Acudientes.");
        }
        return client;
    }

    /**
     * Turns a payment into an invoice line. The amount paid already includes the VAT of its concept, so
     * it is split: value without VAT = amount / (1 + VAT %), rounded to cents, and VAT = amount - value
     * without VAT. The invoice total is therefore exactly what was paid.
     */
    private InvoiceLine lineOf(Payment payment) {
        PaymentConcept concept = getConcept(payment.conceptId());
        BigDecimal rate = concept.taxRate() == null ? BigDecimal.ZERO : concept.taxRate();
        BigDecimal total = payment.amount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal unit = total.multiply(HUNDRED).divide(HUNDRED.add(rate), 2, RoundingMode.HALF_UP);
        return new InvoiceLine(null, null, payment.id(), payment.receiptNumber(), describe(payment, concept), 1, unit,
                rate.setScale(2, RoundingMode.HALF_UP), total.subtract(unit), total);
    }

    /** "Pensión septiembre 2026" (the charge it paid) or "Uniforme - período III 2026". */
    private String describe(Payment payment, PaymentConcept concept) {
        Optional<String> charge = Optional.ofNullable(payment.chargeId()).flatMap(chargeDao::findById).map(Charge::description);
        if (charge.isPresent()) {
            return charge.get();
        }
        String period = payment.period() == null ? "" : " - período " + payment.period();
        return concept.name() + period + " " + (payment.academicYear() == null ? payment.paymentDate().getYear() : payment.academicYear());
    }

    private static BigDecimal sum(List<InvoiceLine> lines, Function<InvoiceLine, BigDecimal> value) {
        return lines.stream().map(value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Everything the renderers need, read again from the database. */
    private InvoiceData invoiceData(Invoice invoice) {
        List<InvoiceLine> lines = invoiceDao.linesOf(invoice.id());
        List<Payment> payments = lines.stream().map(line -> getPayment(line.paymentId())).toList();
        Student student = requireStudent(invoice.studentId());
        Guardian client = guardianDao.findById(invoice.guardianId())
                .orElseThrow(() -> new BusinessException("No existe el acudiente " + invoice.guardianId() + "."));
        String gradeName = enrollmentDao.findByStudentAndYear(student.id(), invoice.issuedAt().getYear())
                .map(Enrollment::gradeName).orElse("-");
        return new InvoiceData(invoice, lines, payments, student, client, gradeName, BillingProfile.from(config));
    }

    /**
     * Writes the invoice files with every generator (TXT and PDF). When the invoice was just issued a
     * failure to write is not an error of the operation: the invoice exists and the files can be
     * generated again later.
     */
    private InvoiceDocument saveFiles(Invoice invoice, boolean justIssued) {
        InvoiceData data = invoiceData(invoice);
        List<String> text = InvoiceTextRenderer.render(data);
        List<Path> files = new ArrayList<>();
        try {
            for (InvoiceGenerator generator : invoiceGenerators) {
                files.add(generator.generate(data));
            }
            Path main = files.getLast(); // the last generator is the PDF (the format required by the specification)
            invoiceDao.updateFile(invoice.id(), main.getFileName().toString(), main.toAbsolutePath().toString());
            return new InvoiceDocument(data, text, files, null);
        } catch (IOException | RuntimeException e) {
            ErrorLogger.error("Invoice files " + invoice.number(), e);
            if (!justIssued) {
                throw new BusinessException("No se pudieron generar los archivos de la factura " + invoice.number() + ": "
                        + e.getMessage());
            }
            return new InvoiceDocument(data, text, files, e.getMessage());
        }
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
