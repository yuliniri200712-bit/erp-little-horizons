package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Charge;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.StudentAdjustment;
import com.littlehorizons.erp.model.report.DebtRow;
import com.littlehorizons.erp.model.dto.InvoiceDocument;
import com.littlehorizons.erp.model.dto.PaymentResult;
import com.littlehorizons.erp.util.Formatter;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import static com.littlehorizons.erp.view.ConsoleView.row;

/** Forms and listings of concepts, adjustments, charges, payments, electronic invoices and arrears. */
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
        console.success("Pago registrado. Recibo " + p.receiptNumber() + " por " + Formatter.money(p.amount()) + " (" + p.method() + ").");
        InvoiceDocument document = result.invoice();
        if (document == null) {
            console.error("El pago quedó registrado, pero no se pudo emitir la factura: " + result.invoiceError()
                    + " Puede emitirla después en 'Facturas electrónicas'.");
            return;
        }
        Invoice i = document.invoice();
        console.success("Factura " + i.number() + " emitida a nombre de " + i.guardianName() + ".");
        console.field("Subtotal (sin IVA)", Formatter.moneyWithCents(i.subtotal()));
        console.field("IVA", Formatter.moneyWithCents(i.tax()));
        console.field("Total", Formatter.moneyWithCents(i.total()));
        console.field("CUFE (simulado)", i.cufe());
        showInvoiceFiles(document);
        console.info("Para verla completa: Facturas electrónicas > Ver una factura por su número.");
    }

    /** Prints the whole invoice, exactly as it is saved in the TXT file. */
    public void printInvoice(InvoiceDocument document) {
        console.info("");
        document.text().forEach(console::info);
        showInvoiceFiles(document);
    }

    public void showInvoiceFiles(InvoiceDocument document) {
        document.files().forEach(file -> console.success("Archivo " + extension(file) + ": " + file.toAbsolutePath()));
        if (document.fileError() != null) {
            console.warning("La factura quedó registrada, pero no se pudieron guardar sus archivos (" + document.fileError()
                    + "). Genérelos de nuevo desde 'Facturas electrónicas'.");
        } else if (document.files().isEmpty()) {
            console.warning("La factura no tiene archivos guardados: genérelos desde 'Facturas electrónicas'.");
        }
        console.warning("La factura es una SIMULACIÓN ACADÉMICA: no está validada ni autorizada por la DIAN.");
    }

    public void showUnbilledPayments(List<Payment> payments) {
        console.table(List.of("N°", "Recibo", "Fecha", "Concepto", "Monto", "Método", "Pagó"),
                IntStream.range(0, payments.size()).mapToObj(n -> {
                    Payment p = payments.get(n);
                    return row(n + 1, p.receiptNumber(), Formatter.date(p.paymentDate()), p.conceptName(),
                            Formatter.money(p.amount()), p.method(), p.guardianName());
                }).toList());
    }

    public void showInvoices(List<Invoice> invoices) {
        console.table(List.of("Número", "Emisión", "Estudiante", "Cliente (acudiente)", "Pagos", "Subtotal", "IVA", "Total"),
                invoices.stream().map(i -> row(i.number(), Formatter.dateTime(i.issuedAt()), i.studentName(), i.guardianName(),
                        i.lineCount(), Formatter.money(i.subtotal()), Formatter.money(i.tax()), Formatter.money(i.total()))).toList());
    }

    private static String extension(Path file) {
        String name = file.getFileName().toString();
        return name.substring(name.lastIndexOf('.') + 1).toUpperCase();
    }

    public void showDebts(List<DebtRow> debts, BigDecimal totalPending) {
        console.table(List.of("Cobro", "Estudiante", "Grado", "Acudiente", "Teléfono", "Concepto", "Vence", "Días mora", "Saldo"),
                debts.stream().map(d -> row(d.chargeId(), d.studentName(), d.gradeName(), d.guardianName(), d.guardianPhone(),
                        d.description(), Formatter.date(d.dueDate()), d.daysOverdue(), Formatter.money(d.pending()))).toList());
        console.info("Total en mora: " + Formatter.money(totalPending));
    }
}
