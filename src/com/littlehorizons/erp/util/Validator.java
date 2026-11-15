package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.BusinessException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;

/** Reusable validation rules. Every failure raises a {@link BusinessException}. */
public final class Validator {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^[0-9+() -]{7,20}$");
    private static final Pattern DOCUMENT = Pattern.compile("^[A-Za-z0-9-]{5,20}$");
    /** Grade with exactly one decimal, e.g. 4.5 or 3,0. */
    private static final Pattern SCORE = Pattern.compile("^\\d\\.\\d$");
    private static final BigDecimal MAX_SCORE = new BigDecimal("5.0");

    private Validator() {
    }

    public static String requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new BusinessException("El campo '" + field + "' es obligatorio.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new BusinessException("El campo '" + field + "' admite máximo " + maxLength + " caracteres.");
        }
        return trimmed;
    }

    public static String optionalText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireText(value, field, maxLength);
    }

    public static String requireDocument(String value) {
        String document = requireText(value, "Número de documento", 20);
        if (!DOCUMENT.matcher(document).matches()) {
            throw new BusinessException("Número de documento inválido (5-20 caracteres alfanuméricos).");
        }
        return document;
    }

    public static String optionalEmail(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireEmail(value);
    }

    public static String requireEmail(String value) {
        String email = requireText(value, "Correo electrónico", 100);
        if (!EMAIL.matcher(email).matches()) {
            throw new BusinessException("Correo electrónico inválido: " + email);
        }
        return email.toLowerCase();
    }

    public static String optionalPhone(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requirePhone(value);
    }

    public static String requirePhone(String value) {
        String phone = requireText(value, "Teléfono", 20);
        if (!PHONE.matcher(phone).matches()) {
            throw new BusinessException("Teléfono inválido (7-20 dígitos): " + phone);
        }
        return phone;
    }

    public static void requireNotFuture(LocalDate date, String field) {
        if (date != null && date.isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha '" + field + "' no puede ser futura.");
        }
    }

    public static void requireOrder(LocalDate from, LocalDate to, String fromField, String toField) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new BusinessException("'" + toField + "' no puede ser anterior a '" + fromField + "'.");
        }
    }

    public static BigDecimal requirePositive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new BusinessException("El campo '" + field + "' debe ser mayor que cero.");
        }
        return value;
    }

    public static int requireRange(int value, int min, int max, String field) {
        if (value < min || value > max) {
            throw new BusinessException("El campo '" + field + "' debe estar entre " + min + " y " + max + ".");
        }
        return value;
    }

    /**
     * Parses a grade written with exactly one decimal (accepts '.' or ',') in the range 0.0-5.0.
     */
    public static BigDecimal parseScore(String raw) {
        if (raw == null) {
            throw new BusinessException("La nota es obligatoria.");
        }
        String normalized = raw.trim().replace(',', '.');
        if (!SCORE.matcher(normalized).matches()) {
            throw new BusinessException("La nota debe tener exactamente un decimal (ejemplo: 4.0 o 3.5).");
        }
        BigDecimal score = new BigDecimal(normalized);
        if (score.compareTo(MAX_SCORE) > 0) {
            throw new BusinessException("La nota debe estar entre 0.0 y 5.0.");
        }
        return score;
    }
}
