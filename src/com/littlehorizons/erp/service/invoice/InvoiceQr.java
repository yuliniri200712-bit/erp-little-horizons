package com.littlehorizons.erp.service.invoice;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.littlehorizons.erp.config.BillingProfile;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.report.InvoiceData;
import com.littlehorizons.erp.util.CufeGenerator;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * QR code of the invoice (SIMULATED content). It carries the same fields as a DIAN QR (number, date,
 * time, NIT, client document, values, CUFE and the verification link) and is a real QR made with
 * ZXing: it is drawn as an image in the PDF and with block characters in the TXT/console version.
 * The link points to the DIAN test environment and will not find the invoice (the data are invented).
 */
public final class InvoiceQr {

    private static final String VERIFY_URL = "https://catalogo-vpfe-hab.dian.gov.co/document/searchqr?documentkey=";
    private static final int TEXT_MARGIN = 2;

    private InvoiceQr() {
    }

    /** The lines of text the QR contains. */
    public static List<String> content(InvoiceData data) {
        Invoice invoice = data.invoice();
        BillingProfile profile = data.profile();
        return List.of(
                "NumFac: " + invoice.number(),
                "FecFac: " + invoice.issuedAt().toLocalDate(),
                "HorFac: " + invoice.issuedAt().toLocalTime() + CufeGenerator.TIME_ZONE,
                "NitFac: " + profile.nitDigits(),
                "DocAdq: " + data.client().documentNumber(),
                "ValFac: " + CufeGenerator.plain(invoice.subtotal()),
                "ValIva: " + CufeGenerator.plain(invoice.tax()),
                "ValOtroIm: 0.00",
                "ValTolFac: " + CufeGenerator.plain(invoice.total()),
                "CUFE: " + invoice.cufe(),
                "QRCode: " + VERIFY_URL + invoice.cufe());
    }

    /** QR modules with a quiet zone of {@code margin} modules (size 0 = one pixel per module). */
    public static BitMatrix matrix(String content, ErrorCorrectionLevel level, int margin, int size) {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, level);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, margin);
        try {
            return new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        } catch (WriterException e) {
            throw new IllegalStateException("No se pudo generar el código QR: " + e.getMessage(), e);
        }
    }

    /**
     * The QR drawn with characters: each character is one module wide and two modules high (upper and
     * lower half blocks), so it looks square in a monospaced font (console, Notepad).
     */
    public static List<String> textRows(String content) {
        BitMatrix m = matrix(content, ErrorCorrectionLevel.L, TEXT_MARGIN, 0);
        List<String> rows = new ArrayList<>();
        for (int y = 0; y < m.getHeight(); y += 2) {
            StringBuilder row = new StringBuilder(m.getWidth());
            for (int x = 0; x < m.getWidth(); x++) {
                boolean top = m.get(x, y);
                boolean bottom = y + 1 < m.getHeight() && m.get(x, y + 1);
                row.append(top && bottom ? '█' : top ? '▀' : bottom ? '▄' : ' ');
            }
            rows.add(row.toString().stripTrailing());
        }
        return rows;
    }
}
