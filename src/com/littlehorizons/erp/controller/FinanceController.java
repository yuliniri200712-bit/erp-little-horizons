package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Charge;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.GuardianLink;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.AdjustmentType;
import com.littlehorizons.erp.model.enums.PaymentMethod;
import com.littlehorizons.erp.model.enums.PaymentPeriod;
import com.littlehorizons.erp.model.report.DebtRow;
import com.littlehorizons.erp.service.BulkChargeResult;
import com.littlehorizons.erp.service.FinanceService;
import com.littlehorizons.erp.service.PaymentRequest;
import com.littlehorizons.erp.service.PaymentResult;
import com.littlehorizons.erp.service.StudentService;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.util.Formatter;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.FinanceView;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Menu 5 - Gestión Financiera: concepts, scholarships, charges, payments, invoices and arrears. */
public class FinanceController extends BaseController {

    private final FinanceService service;
    private final StudentService studentService;
    private final FinanceView financeView;
    private final EntityPicker picker;

    public FinanceController(ConsoleView view, FinanceService service, StudentService studentService, EntityPicker picker) {
        super(view);
        this.service = service;
        this.studentService = studentService;
        this.financeView = new FinanceView(view);
        this.picker = picker;
    }

    @Override
    public void start() {
        runMenu("GESTIÓN FINANCIERA", List.of(
                Option.action("Registrar pago (genera factura PDF)", this::registerPayment),
                Option.action("Historial de pagos de un estudiante", () -> {
                    Student s = picker.student();
                    List<Payment> payments = service.paymentsOfStudent(s.id());
                    financeView.showPayments(payments, service.total(payments));
                }),
                Option.action("Historial de pagos de un acudiente (todos sus hijos)", () -> {
                    Guardian g = picker.guardian();
                    List<Payment> payments = service.paymentsOfGuardian(g.id());
                    financeView.showPayments(payments, service.total(payments));
                }),
                Option.action("Últimos pagos registrados", () -> {
                    List<Payment> payments = service.recentPayments(20);
                    financeView.showPayments(payments, service.total(payments));
                }),
                Option.action("Reporte de deudas con más de un mes de atraso", this::debtReport),
                Option.submenu("Cobros (cuentas por cobrar)", this::chargesMenu),
                Option.submenu("Conceptos de pago", this::conceptsMenu),
                Option.submenu("Becas y descuentos", this::adjustmentsMenu),
                Option.submenu("Facturas y pagos registrados", this::invoicesMenu)));
    }

    // ---------------------------------------------------------------- payments

    private void registerPayment() {
        Student student = picker.student();
        List<Charge> pending = service.chargesOf(student.id(), true);
        Integer chargeId = null;
        int conceptId;
        BigDecimal suggested;
        if (!pending.isEmpty()) {
            view.section("Cobros pendientes");
            financeView.showCharges(pending);
        }
        if (!pending.isEmpty() && view.confirm("¿El pago corresponde a uno de estos cobros?")) {
            Charge charge = view.readValidated("ID del cobro", text -> pending.stream()
                    .filter(c -> String.valueOf(c.id()).equals(text.trim())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("El ID no está en la lista de cobros pendientes.")));
            chargeId = charge.id();
            conceptId = charge.conceptId();
            suggested = charge.pending();
        } else {
            PaymentConcept concept = picker.concept();
            conceptId = concept.id();
            suggested = service.effectiveAmount(student.id(), concept.id());
            view.info("Valor según concepto, becas/descuentos e IVA: " + Formatter.money(suggested));
        }
        BigDecimal amount = view.readMoney("Monto pagado", suggested);
        LocalDate date = view.readDate("Fecha de pago", LocalDate.now());
        PaymentMethod method = view.chooseEnum("Método de pago", PaymentMethod.values(), null);
        PaymentPeriod period = view.chooseOptionalEnum("Período al que aplica", PaymentPeriod.values());
        Integer guardianId = chooseGuardian(student);
        String notes = view.readOptional("Notas", null);

        view.section("Confirmación");
        view.field("Estudiante", student.fullName());
        view.field("Monto", Formatter.money(amount));
        view.field("Método", method);
        if (!view.confirm("¿Confirmar el pago y generar la factura?")) {
            view.warning("Pago no registrado.");
            return;
        }
        PaymentResult result = service.registerPayment(new PaymentRequest(student.id(), guardianId, conceptId, chargeId, amount,
                date, method, period, notes));
        financeView.showPaymentResult(result);
    }

    private Integer chooseGuardian(Student student) {
        List<GuardianLink> links = studentService.guardiansOf(student.id());
        if (links.size() <= 1) {
            return links.isEmpty() ? null : links.getFirst().guardian().id();
        }
        GuardianLink link = view.chooseFrom("Acudiente que realiza el pago", links,
                l -> l.guardian().fullName() + " (" + l.guardian().relationship() + ")"
                        + (l.guardian().financialResponsible() ? " - responsable financiero" : ""));
        return link.guardian().id();
    }

    void debtReport() {
        List<DebtRow> debts = service.debtsOverOneMonth();
        view.info("Cobros pendientes con vencimiento anterior al " + Formatter.date(LocalDate.now().minusMonths(1)) + ":");
        financeView.showDebts(debts, service.totalPending(debts));
    }

    // ---------------------------------------------------------------- charges

    private void chargesMenu() {
        runMenu("COBROS", List.of(
                Option.action("Ver cobros de un estudiante", () -> {
                    Student s = picker.student();
                    financeView.showCharges(service.chargesOf(s.id(), view.confirm("¿Solo pendientes?")));
                }),
                Option.action("Crear cobro a un estudiante", () -> {
                    Student s = picker.student();
                    PaymentConcept concept = picker.concept();
                    view.info("Valor a cobrar (con becas/descuentos e IVA): " + Formatter.money(service.effectiveAmount(s.id(), concept.id())));
                    String description = view.readText("Descripción", concept.name() + " " + currentYear());
                    LocalDate issue = view.readDate("Fecha de emisión", LocalDate.now());
                    LocalDate due = view.readDate("Fecha de vencimiento", issue.plusDays(10));
                    int id = service.createCharge(s.id(), concept.id(), description, issue, due);
                    view.success("Cobro #" + id + " creado.");
                }),
                Option.action("Generar cobro masivo (todos los matriculados)", () -> {
                    PaymentConcept concept = picker.concept();
                    int year = readYear();
                    Integer gradeId = view.confirm("¿Solo para un grado?") ? picker.grade().id() : null;
                    String description = view.readRequired("Descripción (ej: Pensión diciembre " + year + ")");
                    LocalDate issue = view.readDate("Fecha de emisión", LocalDate.now());
                    LocalDate due = view.readDate("Fecha de vencimiento", issue.plusDays(10));
                    BulkChargeResult r = service.generateCharges(concept.id(), description, issue, due, year, gradeId);
                    view.success(r.created() + " cobros creados; " + r.skippedDuplicates() + " ya existían; "
                            + r.skippedFullScholarship() + " omitidos por beca del 100 %.");
                }),
                Option.action("Anular cobro", () -> {
                    Student s = picker.student();
                    financeView.showCharges(service.chargesOf(s.id(), true));
                    int id = view.readInt("ID del cobro", 1, Integer.MAX_VALUE);
                    if (view.confirm("¿Confirma anular el cobro " + id + "?")) {
                        service.cancelCharge(id);
                        view.success("Cobro anulado.");
                    }
                })));
    }

    // ---------------------------------------------------------------- concepts

    private void conceptsMenu() {
        runMenu("CONCEPTOS DE PAGO", List.of(
                Option.action("Catálogo de conceptos", () -> financeView.showConcepts(service.listConcepts())),
                Option.action("Registrar concepto", () -> {
                    int id = service.createConcept(financeView.readConcept(null));
                    view.success("Concepto registrado con ID " + id + ".");
                }),
                Option.action("Actualizar concepto", () -> {
                    service.updateConcept(financeView.readConcept(picker.concept()));
                    view.success("Concepto actualizado.");
                }),
                Option.action("Eliminar concepto", () -> {
                    PaymentConcept concept = picker.concept();
                    if (view.confirm("¿Confirma eliminar " + concept.name() + "?")) {
                        service.deleteConcept(concept.id());
                        view.success("Concepto eliminado.");
                    }
                })));
    }

    // ---------------------------------------------------------------- adjustments

    private void adjustmentsMenu() {
        runMenu("BECAS Y DESCUENTOS", List.of(
                Option.action("Ver becas y descuentos de un estudiante", () ->
                        financeView.showAdjustments(service.adjustmentsOf(picker.student().id()))),
                Option.action("Asignar beca o descuento", () -> {
                    Student s = picker.student();
                    PaymentConcept concept = picker.concept();
                    AdjustmentType type = view.chooseEnum("Tipo", AdjustmentType.values(), null);
                    BigDecimal pct = view.readDecimal("Porcentaje (1-100)", null);
                    String reason = view.readRequired("Motivo");
                    int id = service.addAdjustment(s.id(), concept.id(), type, pct, reason);
                    view.success("Ajuste #" + id + " registrado. Nuevo valor de " + concept.name() + ": "
                            + Formatter.money(service.effectiveAmount(s.id(), concept.id())) + ".");
                }),
                Option.action("Activar / desactivar ajuste", () -> {
                    Student s = picker.student();
                    financeView.showAdjustments(service.adjustmentsOf(s.id()));
                    service.toggleAdjustment(view.readInt("ID del ajuste", 1, Integer.MAX_VALUE));
                    view.success("Estado del ajuste actualizado.");
                }),
                Option.action("Eliminar ajuste", () -> {
                    Student s = picker.student();
                    financeView.showAdjustments(service.adjustmentsOf(s.id()));
                    service.deleteAdjustment(view.readInt("ID del ajuste", 1, Integer.MAX_VALUE));
                    view.success("Ajuste eliminado.");
                })));
    }

    // ---------------------------------------------------------------- invoices & payment maintenance

    private void invoicesMenu() {
        runMenu("FACTURAS Y PAGOS", List.of(
                Option.action("Listar facturas emitidas", () -> financeView.showInvoices(service.listInvoices())),
                Option.action("Generar / regenerar PDF de la factura de un pago", () -> {
                    Payment payment = view.readValidated("ID del pago o número de recibo (ej: RC-000123)", service::findPayment);
                    try {
                        Path pdf = service.generateInvoicePdf(payment.id());
                        view.success("Factura PDF generada en: " + pdf.toAbsolutePath());
                        view.warning("Recuerde: es una SIMULACIÓN ACADÉMICA, sin validación de la DIAN.");
                    } catch (IOException e) {
                        ErrorLogger.error("Invoice PDF for payment " + payment.id(), e);
                        view.error("No se pudo generar el PDF: " + e.getMessage());
                    }
                }),
                Option.action("Actualizar método o notas de un pago", () -> {
                    Payment payment = view.readValidated("ID del pago o número de recibo", service::findPayment);
                    PaymentMethod method = view.chooseEnum("Método de pago", PaymentMethod.values(), payment.method());
                    String notes = view.readOptional("Notas", payment.notes());
                    service.updatePayment(payment.id(), method, notes);
                    view.success("Pago actualizado.");
                }),
                Option.action("Anular (eliminar) un pago", () -> {
                    Payment payment = view.readValidated("ID del pago o número de recibo", service::findPayment);
                    view.warning("Se eliminará el pago " + payment.receiptNumber() + " por " + Formatter.money(payment.amount())
                            + " y su factura; el cobro asociado volverá a quedar pendiente.");
                    if (view.confirm("¿Confirma la anulación?")) {
                        service.deletePayment(payment.id());
                        view.success("Pago anulado.");
                    }
                })));
    }
}
