package com.littlehorizons.erp.service.invoice;

import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.config.BillingProfile;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoiceLine;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.report.InvoiceData;
import com.littlehorizons.erp.util.CufeGenerator;
import com.littlehorizons.erp.util.Formatter;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Saves the invoice as a professional-looking PDF (factura_000123.pdf) with OpenPDF and a QR code made
 * with ZXing. It shows the same data as the TXT version: issuer, client, student, one line per payment,
 * VAT by rate, totals, CUFE, resolution and QR. CUFE, QR and resolution are SIMULATED and the
 * document says so explicitly.
 */
public class PdfInvoiceGenerator implements InvoiceGenerator {

    private static final Color PRIMARY = new Color(0x1F, 0x4E, 0x79);
    private static final Color LIGHT = new Color(0xE8, 0xEF, 0xF7);
    private static final Color WARNING = new Color(0xB0, 0x1E, 0x1E);
    private static final int QR_SIZE = 240;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final String DISCLAIMER = "SIMULACIÓN ACADÉMICA: este documento NO es una factura electrónica validada "
            + "ni autorizada por la DIAN. El CUFE, el código QR y la resolución de habilitación son simulados y no tienen validez fiscal.";

    private final AppConfig config;

    private final Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, PRIMARY);
    private final Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
    private final Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private final Font small = FontFactory.getFont(FontFactory.HELVETICA, 7.5f);
    private final Font white = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
    private final Font warning = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, WARNING);

    public PdfInvoiceGenerator(AppConfig config) {
        this.config = config;
    }

    @Override
    public String format() {
        return "PDF";
    }

    @Override
    public Path pathFor(String invoiceNumber) {
        return config.invoiceOutputDir().toAbsolutePath().resolve(Invoice.baseFileName(invoiceNumber) + ".pdf");
    }

    @Override
    public Path generate(InvoiceData data) throws IOException {
        Path file = pathFor(data.invoice().number());
        Files.createDirectories(file.getParent());
        Document document = new Document(PageSize.LETTER, 40, 40, 36, 36);
        try (OutputStream out = Files.newOutputStream(file)) {
            PdfWriter.getInstance(document, out);
            document.addTitle("Factura " + data.invoice().number() + " (simulación académica)");
            document.addAuthor(data.profile().companyName());
            document.open();
            document.add(header(data));
            document.add(simulationBanner());
            document.add(customer(data));
            document.add(items(data));
            document.add(totals(data));
            document.add(footer(data));
            document.close(); // must happen before the output stream is closed
        } catch (DocumentException e) {
            throw new IOException("No se pudo construir el PDF: " + e.getMessage(), e);
        }
        return file;
    }

    // ---------------------------------------------------------------- sections

    private PdfPTable header(InvoiceData data) {
        BillingProfile profile = data.profile();
        PdfPTable table = new PdfPTable(new float[]{58, 42});
        table.setWidthPercentage(100);
        PdfPCell issuer = new PdfPCell();
        issuer.setBorder(Rectangle.NO_BORDER);
        issuer.addElement(new Paragraph(profile.companyName(), title));
        issuer.addElement(new Paragraph("NIT: " + profile.nit(), bold));
        issuer.addElement(new Paragraph(profile.address(), normal));
        issuer.addElement(new Paragraph("Tel: " + profile.phone() + "   Correo: " + profile.email(), normal));
        issuer.addElement(new Paragraph("Régimen: Responsable de IVA (dato simulado)", small));
        table.addCell(issuer);

        Invoice invoice = data.invoice();
        PdfPTable box = new PdfPTable(1);
        box.addCell(filledCell("FACTURA ELECTRÓNICA DE VENTA (SIMULADA)", white, PRIMARY));
        box.addCell(textCell("No. " + invoice.number(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, PRIMARY)));
        box.addCell(textCell("Emisión: " + invoice.issuedAt().format(STAMP) + " (UTC" + CufeGenerator.TIME_ZONE + ")", normal));
        box.addCell(textCell("Forma de pago: Contado (pagos ya recibidos)", normal));
        box.addCell(textCell("Medio de pago: " + data.payments().stream().map(p -> String.valueOf(p.method()))
                .distinct().collect(Collectors.joining(", ")), normal));
        box.addCell(textCell("Moneda: COP - Peso colombiano", normal));
        PdfPCell right = new PdfPCell(box);
        right.setBorder(Rectangle.NO_BORDER);
        table.addCell(right);
        table.setSpacingAfter(8);
        return table;
    }

    private PdfPTable simulationBanner() {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell(new Phrase(DISCLAIMER, warning));
        cell.setBorderColor(WARNING);
        cell.setBorderWidth(1.2f);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        banner.addCell(cell);
        banner.setSpacingAfter(10);
        return banner;
    }

    private PdfPTable customer(InvoiceData data) {
        Guardian client = data.client();
        Student student = data.student();
        PdfPTable table = new PdfPTable(new float[]{50, 50});
        table.setWidthPercentage(100);
        table.addCell(filledCell("ADQUIRIENTE (ACUDIENTE)", white, PRIMARY));
        table.addCell(filledCell("ESTUDIANTE", white, PRIMARY));
        PdfPCell left = new PdfPCell();
        left.setPadding(6);
        left.addElement(new Paragraph(client.fullName(), bold));
        left.addElement(new Paragraph(client.documentType() + " " + client.documentNumber(), normal));
        left.addElement(new Paragraph("Dirección: " + Formatter.orDash(client.address()), normal));
        left.addElement(new Paragraph("Teléfono: " + Formatter.orDash(client.mainPhone()) + "   Correo: "
                + Formatter.orDash(client.email()), normal));
        table.addCell(left);
        PdfPCell right = new PdfPCell();
        right.setPadding(6);
        right.addElement(new Paragraph(student.fullName(), bold));
        right.addElement(new Paragraph(student.documentType() + " " + student.documentNumber(), normal));
        right.addElement(new Paragraph("Grado: " + Formatter.orDash(data.gradeName()), normal));
        right.addElement(new Paragraph("Código estudiante: " + student.id(), normal));
        table.addCell(right);
        table.setSpacingAfter(10);
        return table;
    }

    private PdfPTable items(InvoiceData data) {
        PdfPTable table = new PdfPTable(new float[]{40, 13, 7, 15, 9, 16});
        table.setWidthPercentage(100);
        table.setHeaderRows(1);
        for (String heading : new String[]{"Descripción", "Recibo", "Cant.", "Valor unitario", "IVA", "Subtotal"}) {
            PdfPCell cell = filledCell(heading, white, PRIMARY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
        for (InvoiceLine line : data.lines()) {
            table.addCell(textCell(line.description(), normal));
            table.addCell(alignedCell(line.receiptNumber(), Element.ALIGN_CENTER));
            table.addCell(alignedCell(String.valueOf(line.quantity()), Element.ALIGN_CENTER));
            table.addCell(alignedCell(Formatter.moneyWithCents(line.unitValue()), Element.ALIGN_RIGHT));
            table.addCell(alignedCell(InvoiceTextRenderer.percent(line.taxRate()), Element.ALIGN_CENTER));
            table.addCell(alignedCell(Formatter.moneyWithCents(line.subtotal()), Element.ALIGN_RIGHT));
        }
        return table;
    }

    private PdfPTable totals(InvoiceData data) {
        Invoice invoice = data.invoice();
        PdfPTable table = new PdfPTable(new float[]{60, 22, 18});
        table.setWidthPercentage(100);
        Map<String, BigDecimal> rows = new LinkedHashMap<>();
        rows.put("Subtotal (sin IVA)", invoice.subtotal());
        InvoiceTextRenderer.taxByRate(data.lines()).forEach((rate, amount) ->
                rows.put("IVA " + InvoiceTextRenderer.percent(rate) + (rate.signum() == 0 ? " (excluido)" : ""), amount));
        rows.put("TOTAL PAGADO", invoice.total());
        rows.forEach((label, value) -> {
            boolean total = label.startsWith("TOTAL");
            PdfPCell empty = new PdfPCell(new Phrase(""));
            empty.setBorder(Rectangle.NO_BORDER);
            table.addCell(empty);
            table.addCell(total ? filledCell(label, white, PRIMARY) : filledCell(label, bold, LIGHT));
            PdfPCell amount = total ? filledCell(Formatter.moneyWithCents(value), white, PRIMARY)
                    : textCell(Formatter.moneyWithCents(value), normal);
            amount.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(amount);
        });
        table.setSpacingBefore(4);
        table.setSpacingAfter(14);
        return table;
    }

    private PdfPTable footer(InvoiceData data) throws IOException {
        Invoice invoice = data.invoice();
        PdfPTable table = new PdfPTable(new float[]{28, 72});
        table.setWidthPercentage(100);
        Image qr = qrImage(String.join("\n", InvoiceQr.content(data)));
        qr.scaleToFit(140, 140);
        PdfPCell qrCell = new PdfPCell(qr);
        qrCell.setPadding(4);
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(qrCell);

        PdfPCell info = new PdfPCell();
        info.setPadding(6);
        info.addElement(new Paragraph("CUFE (SHA-384, SIMULADO):", bold));
        info.addElement(new Paragraph(invoice.cufe(), small));
        info.addElement(new Paragraph("Ambiente: " + BillingProfile.ENVIRONMENT_LABEL, normal));
        info.addElement(new Paragraph(invoice.resolution(), normal));
        info.addElement(new Paragraph("Código QR con los datos de la factura y el enlace de verificación del ambiente "
                + "de pruebas de la DIAN (la factura no existe allí: los datos son simulados).", small));
        info.addElement(new Paragraph(" ", small));
        info.addElement(new Paragraph(DISCLAIMER, warning));
        table.addCell(info);
        return table;
    }

    // ---------------------------------------------------------------- helpers

    private static Image qrImage(String content) throws IOException {
        BitMatrix matrix = InvoiceQr.matrix(content, ErrorCorrectionLevel.M, 1, QR_SIZE);
        BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < matrix.getWidth(); x++) {
            for (int y = 0; y < matrix.getHeight(); y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        return Image.getInstance(png.toByteArray());
    }

    private static PdfPCell filledCell(String text, Font font, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(background);
        cell.setPadding(5);
        return cell;
    }

    private static PdfPCell textCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell alignedCell(String text, int alignment) {
        PdfPCell cell = textCell(text, normal);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }
}
