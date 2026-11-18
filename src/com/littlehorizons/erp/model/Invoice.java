package com.littlehorizons.erp.model;

import com.littlehorizons.erp.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Electronic invoice (SIMULATED) issued to ONE guardian (the client) for one or more payments of ONE
 * student. Number, CUFE and resolution are assigned by the system when it is issued. Table: facturas
 * (its lines are in detalle_factura, see {@link InvoiceLine}).
 */
public record Invoice(
        Integer id,
        String number,
        int studentId,
        String studentName,
        int guardianId,
        String guardianName,
        LocalDateTime issuedAt,
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal total,
        String cufe,
        String resolution,
        String fileName,
        String filePath,
        int lineCount) {

    public static final String PREFIX = "FE";
    /** Maximum number of payments (lines) in one invoice. */
    public static final int MAX_LINES = 30;

    /** FE-000123 for the consecutive 123. */
    public static String numberOf(int sequence) {
        return String.format("%s-%06d", PREFIX, sequence);
    }

    /** Accepts "13", "fe-13" or "FE-000013" and returns "FE-000013"; anything else is rejected. */
    public static String normalizeNumber(String text) {
        String clean = text == null ? "" : text.trim().toUpperCase().replace(" ", "");
        if (clean.startsWith(PREFIX + "-")) {
            clean = clean.substring(PREFIX.length() + 1);
        }
        if (!clean.matches("\\d{1,6}") || Integer.parseInt(clean) == 0) {
            throw new BusinessException("Escriba el número de la factura, por ejemplo 13 o FE-000013.");
        }
        return numberOf(Integer.parseInt(clean));
    }

    /** The consecutive inside "FE-000123" (123). */
    public static int sequenceOf(String number) {
        return Integer.parseInt(normalizeNumber(number).substring(PREFIX.length() + 1));
    }

    /** factura_000123 (without extension): unique because the consecutive is unique. */
    public static String baseFileName(String number) {
        return "factura_" + normalizeNumber(number).substring(PREFIX.length() + 1);
    }
}
