package com.littlehorizons.erp.service.invoice;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.report.InvoiceData;
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
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

/**
 * Renders a professional-looking invoice PDF with OpenPDF and a QR code with ZXing.
 * CUFE, QR and resolution are SIMULATED and the document says so explicitly.
 */
public class PdfInvoiceGenerator implements InvoiceGenerator {

    private static final Color PRIMARY = new Color(0x1F, 0x4E, 0x79);
    private static final Color LIGHT = new Color(0xE8, 0xEF, 0xF7);
    private static final Color WARNING = new Color(0xB0, 0x1E, 0x1E);
    private static final int QR_SIZE = 180;
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
    public Path generate(InvoiceData data) throws IOException {
        Path folder = config.invoiceOutputDir().toAbsolutePath();
        Files.createDirectories(folder);
        Path file = folder.resolve(fileNameFor(data.invoice().number()));
        Document document = new Document(PageSize.LETTER, 40, 40, 36, 36);
        try (OutputStream out = Files.newOutputStream(file)) {
            PdfWriter.getInstance(document, out);
            document.addTitle("Factura " + data.invoice().number() + " (simulación académica)");
            document.addAuthor(config.companyName());
            document.open();
            document.add(header(data));
            document.add(simulationBanner());
            document.add(customer(data));
            document.add(items(data));
            document.add(totals(data.invoice(), data.concept().taxRate()));
            document.add(footer(data));
            document.close(); // must happen before the output stream is closed
        } catch (DocumentException e) {
            throw new IOException("No se pudo construir el PDF: " + e.getMessage(), e);
        }
        return file;
    }

    /** factura_000123.pdf from invoice number FE-000123. */
    public static String fileNameFor(String invoiceNumber) {
        String digits = invoiceNumber.replaceAll("\\D", "");
        return "factura_" + digits + ".pdf";
    }

    // ---------------------------------------------------------------- sections

    private PdfPTable header(InvoiceData data) {
        PdfPTable table = new PdfPTable(new float[]{60, 40});
        table.setWidthPercentage(100);
        PdfPCell issuer = new PdfPCell();
        issuer.setBorder(Rectangle.NO_BORDER);
        issuer.addElement(new Paragraph(config.companyName(), title));
        issuer.addElement(new Paragraph("NIT: " + config.companyNit(), bold));
        issuer.addElement(new Paragraph(config.companyAddress(), normal));
        issuer.addElement(new Paragraph("Tel: " + config.companyPhone() + "   Correo: " + config.companyEmail(), normal));
        issuer.addElement(new Paragraph("Régimen: Responsable de IVA (dato simulado)", small));
        table.addCell(issuer);

        Invoice invoice = data.invoice();
        Payment payment = data.payment();
        PdfPTable box = new PdfPTable(1);
        box.addCell(filledCell("FACTURA ELECTRÓNICA DE VENTA (SIMULADA)", white, PRIMARY));
        box.addCell(textCell("No. " + invoice.number(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, PRIMARY)));
        box.addCell(textCell("Fecha de emisión: " + Formatter.date(invoice.issueDate()), normal));
        box.addCell(textCell("Fecha de pago: " + Formatter.date(payment.paymentDate()), normal));
        box.addCell(textCell("Recibo de caja: " + payment.receiptNumber(), normal));
        box.addCell(textCell("Medio de pago: " + payment.method(), normal));
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
        Guardian guardian = data.guardian();
        Student student = data.student();
        PdfPTable table = new PdfPTable(new float[]{50, 50});
        table.setWidthPercentage(100);
        table.addCell(filledCell("ADQUIRIENTE (ACUDIENTE RESPONSABLE)", white, PRIMARY));
        table.addCell(filledCell("ESTUDIANTE", white, PRIMARY));
        PdfPCell left = new PdfPCell();
        left.setPadding(6);
        if (guardian != null) {
            left.addElement(new Paragraph(guardian.fullName(), bold));
            left.addElement(new Paragraph(guardian.documentType() + " " + guardian.documentNumber(), normal));
            left.addElement(new Paragraph("Dirección: " + Formatter.orDash(guardian.address()), normal));
            left.addElement(new Paragraph("Teléfono: " + guardian.mainPhone() + "   Correo: " + Formatter.orDash(guardian.email()), normal));
        } else {
            left.addElement(new Paragraph("Sin acudiente registrado", normal));
        }
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
        Invoice invoice = data.invoice();
        PdfPTable table = new PdfPTable(new float[]{52, 12, 18, 18});
        table.setWidthPercentage(100);
        for (String heading : new String[]{"Descripción", "Cantidad", "Precio unitario", "Subtotal"}) {
            PdfPCell cell = filledCell(heading, white, PRIMARY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
        }
        table.addCell(textCell(data.lineDescription(), normal));
        table.addCell(alignedCell("1", Element.ALIGN_CENTER));
        table.addCell(alignedCell(Formatter.money(invoice.subtotal()), Element.ALIGN_RIGHT));
        table.addCell(alignedCell(Formatter.money(invoice.subtotal()), Element.ALIGN_RIGHT));
        return table;
    }

    private PdfPTable totals(Invoice invoice, BigDecimal taxRate) {
        PdfPTable table = new PdfPTable(new float[]{64, 18, 18});
        table.setWidthPercentage(100);
        String rate = taxRate == null ? "0" : taxRate.setScale(0, RoundingMode.HALF_UP).toPlainString();
        Map<String, BigDecimal> rows = new java.util.LinkedHashMap<>();
        rows.put("Subtotal", invoice.subtotal());
        rows.put("IVA (" + rate + "%)" + (invoice.tax().signum() == 0 ? " - servicio excluido" : ""), invoice.tax());
        rows.put("TOTAL A PAGAR", invoice.total());
        rows.forEach((label, value) -> {
            boolean total = label.startsWith("TOTAL");
            PdfPCell empty = new PdfPCell(new Phrase(""));
            empty.setBorder(Rectangle.NO_BORDER);
            table.addCell(empty);
            PdfPCell name = total ? filledCell(label, white, PRIMARY) : filledCell(label, bold, LIGHT);
            table.addCell(name);
            PdfPCell amount = total ? filledCell(Formatter.money(value), white, PRIMARY) : textCell(Formatter.money(value), normal);
            amount.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(amount);
        });
        table.setSpacingBefore(4);
        table.setSpacingAfter(14);
        return table;
    }

    private PdfPTable footer(InvoiceData data) throws IOException {
        Invoice invoice = data.invoice();
        PdfPTable table = new PdfPTable(new float[]{24, 76});
        table.setWidthPercentage(100);
        Image qr = qrImage(qrContent(data));
        qr.scaleToFit(110, 110);
        PdfPCell qrCell = new PdfPCell(qr);
        qrCell.setPadding(4);
        qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(qrCell);

        PdfPCell info = new PdfPCell();
        info.setPadding(6);
        info.addElement(new Paragraph("CUFE (SIMULADO):", bold));
        info.addElement(new Paragraph(invoice.cufe(), small));
        info.addElement(new Paragraph(" ", small));
        info.addElement(new Paragraph(config.invoiceResolution(), normal));
        info.addElement(new Paragraph("Código QR simulado con los datos de la factura (no verificable en el portal DIAN).", small));
        info.addElement(new Paragraph(" ", small));
        info.addElement(new Paragraph(DISCLAIMER, warning));
        table.addCell(info);
        return table;
    }

    // ---------------------------------------------------------------- helpers

    private String qrContent(InvoiceData data) {
        Invoice invoice = data.invoice();
        Map<QrField, String> fields = new EnumMap<>(QrField.class);
        fields.put(QrField.NUMBER, invoice.number());
        fields.put(QrField.DATE, invoice.issueDate().toString());
        fields.put(QrField.ISSUER_NIT, config.companyNit());
        fields.put(QrField.CUSTOMER_DOC, data.guardian() == null ? "-" : data.guardian().documentNumber());
        fields.put(QrField.SUBTOTAL, invoice.subtotal().toPlainString());
        fields.put(QrField.TAX, invoice.tax().toPlainString());
        fields.put(QrField.TOTAL, invoice.total().toPlainString());
        fields.put(QrField.CUFE, invoice.cufe());
        StringBuilder content = new StringBuilder();
        fields.forEach((field, value) -> content.append(field.label).append(": ").append(value).append('\n'));
        content.append("SIMULACION ACADEMICA - SIN VALIDEZ DIAN");
        return content.toString();
    }

    private enum QrField {
        NUMBER("NumFac"), DATE("FecFac"), ISSUER_NIT("NitFac"), CUSTOMER_DOC("DocAdq"), SUBTOTAL("ValFac"), TAX("ValIva"),
        TOTAL("ValTotal"), CUFE("CUFE");

        private final String label;

        QrField(String label) {
            this.label = label;
        }
    }

    private static Image qrImage(String content) throws IOException {
        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, hints);
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
                }
            }
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(image, "png", png);
            return Image.getInstance(png.toByteArray());
        } catch (WriterException e) {
            throw new IOException("No se pudo generar el código QR: " + e.getMessage(), e);
        }
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
