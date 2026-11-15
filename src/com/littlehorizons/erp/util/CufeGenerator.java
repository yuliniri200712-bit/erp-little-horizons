package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;

/**
 * SIMULATED CUFE (academic exercise). It is a SHA-384 hash of
 * {@code invoiceNumber|issuerNitDigits|issueDate|total}; it is NOT the algorithm nor the data set
 * defined by DIAN and it has no fiscal validity.
 */
public final class CufeGenerator {

    private CufeGenerator() {
    }

    public static String simulatedCufe(String invoiceNumber, String issuerNit, LocalDate issueDate, BigDecimal total) {
        String nitDigits = issuerNit.replaceAll("\\D", "");
        String source = invoiceNumber + "|" + nitDigits + "|" + issueDate + "|" + total.setScale(2, RoundingMode.HALF_UP).toPlainString();
        try {
            byte[] hash = MessageDigest.getInstance("SHA-384").digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-384 no disponible en la JVM", e);
        }
    }
}
