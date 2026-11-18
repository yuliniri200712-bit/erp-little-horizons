package com.littlehorizons.erp.service.invoice;

import com.littlehorizons.erp.config.BillingProfile;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoiceLine;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.report.InvoiceData;
import com.littlehorizons.erp.util.CufeGenerator;
import com.littlehorizons.erp.util.Formatter;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Builds the text of the electronic invoice, line by line (78 columns, monospaced font). It is a pure
 * function of the saved data: the same invoice always gives the same lines, so it is printed in the
 * console, saved as the TXT file and can be regenerated at any time with identical content.
 */
public final class InvoiceTextRenderer {

    public static final int WIDTH = 78;
    private static final int DESCRIPTION = 40;
    private static final int QUANTITY = 5;
    private static final int UNIT = 15;
    private static final int SUBTOTAL = 15; // 40 + 1 + 5 + 1 + 15 + 1 + 15 = 78
    private static final int TOTAL = 20;
    private static final int LABEL = 17;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private InvoiceTextRenderer() {
    }

    public static List<String> render(InvoiceData data) {
        Invoice invoice = data.invoice();
        BillingProfile profile = data.profile();
        String heavy = "=".repeat(WIDTH);
        String light = "-".repeat(WIDTH);
        List<String> out = new ArrayList<>();

        out.add(heavy);
        out.add(centered(profile.companyName()));
        out.add(centered("NIT " + profile.nit()));
        out.add(centered(profile.address()));
        out.add(centered("Tel. " + profile.phone() + "  |  " + profile.email()));
        out.add(heavy);
        out.add(centered("FACTURA ELECTRÓNICA DE VENTA"));
        out.add(centered("No. " + invoice.number()));
        out.add(centered("SIMULACIÓN ACADÉMICA - SIN VALIDEZ ANTE LA DIAN"));
        out.add(heavy);
        out.add(field("Fecha de emisión", invoice.issuedAt().format(STAMP) + " (hora Colombia, UTC" + CufeGenerator.TIME_ZONE + ")"));
        out.add(field("Forma de pago", "Contado (pagos ya recibidos)"));
        out.add(field("Medio de pago", paymentMethods(data.payments())));
        out.add(field("Moneda", "COP - Peso colombiano"));
        out.add(light);

        Guardian client = data.client();
        Student student = data.student();
        out.add("ADQUIRIENTE (ACUDIENTE)");
        out.add(field("Nombre", client.fullName()));
        out.add(field("Documento", client.documentType() + " " + client.documentNumber()));
        out.add(field("Dirección", client.address()));
        out.add(field("Teléfono", client.mainPhone()));
        out.add(field("Correo", client.email()));
        out.add(field("Estudiante", student.fullName() + " (" + student.documentType() + " " + student.documentNumber() + ")"));
        out.add(field("Grado", data.gradeName()));
        out.add(light);

        out.add("DETALLE");
        out.add(pad("Descripción", DESCRIPTION) + " " + padLeft("Cant.", QUANTITY) + " " + padLeft("Valor unitario", UNIT)
                + " " + padLeft("Subtotal", SUBTOTAL));
        out.add(light);
        for (InvoiceLine line : data.lines()) {
            List<String> description = wrap(line.description(), DESCRIPTION);
            out.add(pad(description.getFirst(), DESCRIPTION) + " " + padLeft(String.valueOf(line.quantity()), QUANTITY) + " "
                    + padLeft(Formatter.moneyWithCents(line.unitValue()), UNIT) + " "
                    + padLeft(Formatter.moneyWithCents(line.subtotal()), SUBTOTAL));
            description.stream().skip(1).forEach(out::add);
            out.add("  Recibo " + line.receiptNumber() + " | IVA " + percent(line.taxRate()) + " | Total línea "
                    + Formatter.moneyWithCents(line.lineTotal()));
        }
        out.add(light);
        out.add(total("Subtotal (sin IVA)", invoice.subtotal()));
        taxByRate(data.lines()).forEach((rate, amount) -> out.add(total("IVA " + percent(rate), amount)));
        out.add(total("TOTAL PAGADO (COP)", invoice.total()));
        out.add(heavy);

        out.add("ELEMENTOS DE LA FACTURA ELECTRÓNICA (SIMULADOS)");
        out.add("CUFE (SHA-384):");
        out.add("  " + invoice.cufe().substring(0, 48));
        out.add("  " + invoice.cufe().substring(48));
        out.add(field("Ambiente", BillingProfile.ENVIRONMENT_LABEL));
        out.add("Resolución de habilitación de facturación:");
        wrap(invoice.resolution(), WIDTH - 2).forEach(part -> out.add("  " + part));
        out.add(light);

        List<String> qr = InvoiceQr.content(data);
        out.add("CÓDIGO QR - contenido (escanéelo con el celular):");
        qr.forEach(part -> wrap(part, WIDTH - 2).forEach(piece -> out.add("  " + piece)));
        out.add("");
        InvoiceQr.textRows(String.join("\n", qr)).forEach(row -> out.add(centered(row)));
        out.add("");
        out.add(heavy);
        out.add(centered("Documento académico: no es una factura real ni se envía a la DIAN."));
        out.add(heavy);
        return out;
    }

    /** VAT grouped by rate (0 %, 19 %...), ascending, so lines with different rates are shown apart. */
    static Map<BigDecimal, BigDecimal> taxByRate(List<InvoiceLine> lines) {
        return lines.stream().collect(Collectors.groupingBy(l -> l.taxRate().stripTrailingZeros(), TreeMap::new,
                Collectors.reducing(BigDecimal.ZERO, InvoiceLine::taxAmount, BigDecimal::add)));
    }

    static String percent(BigDecimal rate) {
        return rate.stripTrailingZeros().toPlainString() + "%";
    }

    private static String paymentMethods(List<Payment> payments) {
        return payments.stream().map(p -> String.valueOf(p.method())).distinct().collect(Collectors.joining(", "));
    }

    private static String total(String label, BigDecimal amount) {
        return padLeft(label + ":", WIDTH - TOTAL - 1) + " " + padLeft(Formatter.moneyWithCents(amount), TOTAL);
    }

    private static String field(String label, String value) {
        return pad(label, LABEL) + ": " + (value == null || value.isBlank() ? "(no registrado)" : value);
    }

    private static String centered(String text) {
        return " ".repeat(Math.max(0, (WIDTH - text.length()) / 2)) + text;
    }

    private static String pad(String text, int width) {
        return text.length() >= width ? text : text + " ".repeat(width - text.length());
    }

    private static String padLeft(String text, int width) {
        return text.length() >= width ? text : " ".repeat(width - text.length()) + text;
    }

    /** Cuts the text at spaces so no line is longer than {@code width} (longer words are cut). */
    static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : (text == null ? "" : text).split(" ")) {
            while (word.length() > width) {
                if (!current.isEmpty()) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (!current.isEmpty() && current.length() + 1 + word.length() > width) {
                lines.add(current.toString());
                current.setLength(0);
            }
            current.append(current.isEmpty() ? "" : " ").append(word);
        }
        lines.add(current.toString());
        return lines;
    }
}
