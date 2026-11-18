package com.littlehorizons.erp.config;

import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Invoice;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * Issuer data printed on every invoice and the (SIMULATED) DIAN invoicing authorization: resolution
 * number and date, authorized numbering range and technical key. Everything is fictional and comes
 * from the {@code company.*} and {@code invoice.*} keys of {@code config/app.properties}. It is read
 * when an invoice is issued, so a wrong value is reported as an error of that operation.
 */
public record BillingProfile(String companyName, String nit, String address, String phone, String email,
                             String resolutionNumber, LocalDate resolutionDate, int rangeFrom, int rangeTo,
                             int validMonths, String technicalKey) {

    /** DIAN environment code 2 = habilitación (tests). This program never issues production invoices. */
    public static final String ENVIRONMENT = "2";
    public static final String ENVIRONMENT_LABEL = ENVIRONMENT + " - Habilitación (pruebas)";

    private static final Pattern NIT = Pattern.compile("\\d{3}\\.?\\d{3}\\.?\\d{3}-\\d");
    private static final Pattern RESOLUTION_NUMBER = Pattern.compile("\\d{10,14}");

    public static BillingProfile from(AppConfig config) {
        String nit = config.companyNit().trim();
        if (!NIT.matcher(nit).matches()) {
            throw invalid("company.nit debe tener 9 dígitos, guion y dígito de verificación (ej: 901.234.567-8), no '" + nit + "'.");
        }
        String resolution = config.get("invoice.resolution.number", "18760000000001").trim();
        if (!RESOLUTION_NUMBER.matcher(resolution).matches()) {
            throw invalid("invoice.resolution.number debe tener entre 10 y 14 dígitos, no '" + resolution + "'.");
        }
        int from = integer(config, "invoice.resolution.from", "1");
        int to = integer(config, "invoice.resolution.to", "100000");
        if (from < 1 || to < from || to > 999_999) {
            throw invalid("invoice.resolution.from y invoice.resolution.to deben formar un rango entre 1 y 999999.");
        }
        int months = integer(config, "invoice.resolution.months", "24");
        if (months < 1 || months > 120) {
            throw invalid("invoice.resolution.months debe estar entre 1 y 120.");
        }
        return new BillingProfile(config.companyName(), nit, config.companyAddress(), config.companyPhone(),
                config.companyEmail(), resolution, date(config, "invoice.resolution.date", "2026-01-15"), from, to, months,
                text(config, "invoice.technical.key", "3b9f7c1e-52d4-4a8b-9e06-7d2c4f1a8e35"));
    }

    /** NIT without separators nor verification digit (901234567), as the CUFE and the QR use it. */
    public String nitDigits() {
        return nit.substring(0, nit.indexOf('-')).replace(".", "");
    }

    public LocalDate validUntil() {
        return resolutionDate.plusMonths(validMonths);
    }

    /** Text saved with each invoice and printed on it. */
    public String resolutionText() {
        return "Resolución DIAN No. " + resolutionNumber + " del " + resolutionDate + ", prefijo " + Invoice.PREFIX
                + ", rango " + Invoice.numberOf(rangeFrom) + " a " + Invoice.numberOf(rangeTo) + ", vigente hasta "
                + validUntil() + " (SIMULADA)";
    }

    /** Invoices can only be issued while the resolution is in force. */
    public void requireValidOn(LocalDate date) {
        if (date.isBefore(resolutionDate)) {
            throw new BusinessException("La resolución de facturación empieza el " + resolutionDate
                    + ": todavía no se pueden emitir facturas.");
        }
        if (date.isAfter(validUntil())) {
            throw new BusinessException("La resolución de facturación venció el " + validUntil()
                    + ". Configure una nueva resolución (invoice.resolution.*) para emitir facturas.");
        }
    }

    /** The consecutive number must be inside the range authorized by the resolution. */
    public void requireInRange(String number) {
        int sequence = Invoice.sequenceOf(number);
        if (sequence < rangeFrom || sequence > rangeTo) {
            throw new BusinessException("El rango autorizado por la resolución (" + Invoice.numberOf(rangeFrom) + " a "
                    + Invoice.numberOf(rangeTo) + ") no incluye el número " + number + ". Solicite una nueva resolución.");
        }
    }

    private static BusinessException invalid(String detail) {
        return new BusinessException("Configuración de facturación inválida en config/app.properties: " + detail);
    }

    private static String text(AppConfig config, String key, String defaultValue) {
        String value = config.get(key, defaultValue).trim();
        if (value.isEmpty()) {
            throw invalid(key + " no puede estar vacío.");
        }
        return value;
    }

    private static int integer(AppConfig config, String key, String defaultValue) {
        String value = config.get(key, defaultValue).trim();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw invalid(key + " debe ser un número entero, no '" + value + "'.");
        }
    }

    private static LocalDate date(AppConfig config, String key, String defaultValue) {
        String value = config.get(key, defaultValue).trim();
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw invalid(key + " debe ser una fecha aaaa-mm-dd (ej: 2026-01-15), no '" + value + "'.");
        }
    }
}
