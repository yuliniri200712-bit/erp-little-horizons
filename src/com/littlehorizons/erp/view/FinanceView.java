package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Charge;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.StudentAdjustment;
import com.littlehorizons.erp.model.report.DebtRow;
import com.littlehorizons.erp.service.PaymentResult;
import com.littlehorizons.erp.util.Formatter;

import java.math.BigDecimal;
import java.util.List;

import static com.littlehorizons.erp.view.ConsoleView.row;

/** Forms and listings of concepts, adjustments, charges, payments, invoices and arrears. */
public class FinanceView {

    private final ConsoleView console;

    public FinanceView(ConsoleView console) {
        this.console = console;
    }

    public PaymentConcept readConcept(PaymentConcept current) {
        boolean isNew = current == null;
        console.section(isNew ? "Nuevo concepto de pago" : "Actualizar concepto (Enter conserva el valor)");
        String code = console.readText("Código (ej: PENSION)", isNew ? null : current.code());
        String name = console.readText("Nombre", isNew ? null : current.name());
        String description = console.readOptional("Descripción", isNew ? null : current.description());
        BigDecimal base = console.readMoney("Valor base (COP, antes de IVA)", isNew ? null : current.baseAmount());
        BigDecimal tax = console.readDecimal("Porcentaje de IVA (0 si es servicio educativo excluido)", isNew ? BigDecimal.ZERO : current.taxRate());
        boolean recurring = console.readBoolean("¿Es recurrente (mensual)?", !isNew && current.recurring());
        boolean active = console.readBoolean("¿Activo?", isNew || current.active());
        return new PaymentConcept(isNew ? null : current.id(), code, name, description, base, tax, recurring, active);
    }

    public void showConcepts(List<PaymentConcept> concepts) {
        console.table(List.of("ID", "Código", "Concepto", "Valor base", "IVA %", "Recurrente", "Activo"),
                concepts.stream().map(c -> row(c.id(), c.code(), c.name(), Formatter.money(c.baseAmount()),
                        c.taxRate().stripTrailingZeros().toPlainString(), Formatter.yesNo(c.recurring()), Formatter.yesNo(c.active()))).toList());
    }

    public void showAdjustments(List<StudentAdjustment> adjustments) {
        console.table(List.of("ID", "Concepto", "Tipo", "Porcentaje", "Motivo", "Activo"),
                adjustments.stream().map(a -> row(a.id(), a.conceptName(), a.type(), a.percentage().stripTrailingZeros().toPlainString() + "%",
                        a.reason(), Formatter.yesNo(a.active()))).toList());
    }

    public void showCharges(List<Charge> charges) {
        console.table(List.of("ID", "Concepto", "Descripción", "Valor", "Abonado", "Saldo", "Vence", "Estado"),
                charges.stream().map(c -> row(c.id(), c.conceptName(), c.description(), Formatter.money(c.amount()),
                        Formatter.money(c.paidAmount()), Formatter.money(c.pending()), Formatter.date(c.dueDate()), c.status())).toList());
    }

    public void showPayments(List<Payment> payments, BigDecimal total) {
        console.table(List.of("ID", "Recibo", "Fecha", "Estudiante", "Concepto", "Monto", "Método", "Pagó", "Período"),
                payments.stream().map(p -> row(p.id(), p.receiptNumber(), Formatter.date(p.paymentDate()), p.studentName(),
                        p.conceptName(), Formatter.money(p.amount()), p.method(), p.guardianName(),
                        p.period() == null ? "-" : p.academicYear() + " " + p.period())).toList());
        console.info("Total pagado: " + Formatter.money(total));
    }

    public void showPaymentResult(PaymentResult result) {
        Payment p = result.payment();
        Invoice i = result.invoice();
        console.success("Pago registrado. Recibo " + p.receiptNumber() + " por " + Formatter.money(p.amount()) + " (" + p.method() + ").");
        console.field("Factura", i.number());
        console.field("Subtotal", Formatter.money(i.subtotal()));
        console.field("IVA", Formatter.money(i.tax()));
        console.field("Total", Formatter.money(i.total()));
        console.field("CUFE (simulado)", i.cufe());
        if (result.pdfPath() != null) {
            console.success("Factura PDF generada en: " + result.pdfPath().toAbsolutePath());
        } else {
            console.warning("El pago quedó registrado pero el PDF no se pudo generar: " + result.pdfError()
                    + ". Puede regenerarlo desde 'Facturas'.");
        }
        console.warning("La factura es una SIMULACIÓN ACADÉMICA: no está validada ni autorizada por la DIAN.");
    }

    public void showInvoices(List<Invoice> invoices) {
        console.table(List.of("ID", "Número", "ID pago", "Emisión", "Subtotal", "IVA", "Total", "Archivo PDF"),
                invoices.stream().map(i -> row(i.id(), i.number(), i.paymentId(), Formatter.date(i.issueDate()), Formatter.money(i.subtotal()),
                        Formatter.money(i.tax()), Formatter.money(i.total()), i.fileName() == null ? "(no generado)" : i.fileName())).toList());
    }

    public void showDebts(List<DebtRow> debts, BigDecimal totalPending) {
        console.table(List.of("Cobro", "Estudiante", "Grado", "Acudiente", "Teléfono", "Concepto", "Vence", "Días mora", "Saldo"),
                debts.stream().map(d -> row(d.chargeId(), d.studentName(), d.gradeName(), d.guardianName(), d.guardianPhone(),
                        d.description(), Formatter.date(d.dueDate()), d.daysOverdue(), Formatter.money(d.pending()))).toList());
        console.info("Total en mora: " + Formatter.money(totalPending));
    }
}
