package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * SIMULATED CUFE (Código Único de Factura Electrónica), academic exercise. Like the real one, it is the
 * SHA-384 hash of the invoice data joined in a fixed order: number, date, time with offset, subtotal,
 * VAT (tax code 01), other taxes (codes 04 and 03, always zero here), total, issuer NIT, client document,
 * technical key and environment. Same data, same CUFE; change one cent and it changes. It has NO fiscal
 * validity: the technical key and the resolution are invented and nothing is sent to the DIAN.
 */
public final class CufeGenerator {

    /** Colombia has no daylight saving time: the offset is always -05:00. */
    public static final String TIME_ZONE = "-05:00";

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private CufeGenerator() {
    }

    /** SHA-384 in lowercase hexadecimal: always 96 characters. */
    public static String simulatedCufe(String number, LocalDateTime issuedAt, BigDecimal subtotal, BigDecimal tax,
                                       BigDecimal total, String issuerNitDigits, String clientDocument,
                                       String technicalKey, String environment) {
        String source = number
                + issuedAt.format(DATE)
                + issuedAt.format(TIME) + TIME_ZONE
                + plain(subtotal)
                + "01" + plain(tax)
                + "04" + plain(BigDecimal.ZERO)
                + "03" + plain(BigDecimal.ZERO)
                + plain(total)
                + issuerNitDigits
                + clientDocument
                + technicalKey
                + environment;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-384").digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-384 no disponible en la JVM", e);
        }
    }

    /** 350000.00: two decimals, no grouping (the format used inside the CUFE and the QR). */
    public static String plain(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
