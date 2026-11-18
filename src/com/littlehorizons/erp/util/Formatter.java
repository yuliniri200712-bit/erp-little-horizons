package com.littlehorizons.erp.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatting helpers for Colombian pesos, dates and averages. */
public final class Formatter {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale COLOMBIA = Locale.of("es", "CO");

    private Formatter() {
    }

    public static String money(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(COLOMBIA);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        return "$ " + format.format(amount.setScale(0, RoundingMode.HALF_UP));
    }

    /** Money with cents, as printed on invoices: $ 297.500,00. */
    public static String moneyWithCents(BigDecimal amount) {
        if (amount == null) {
            return "-";
        }
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(COLOMBIA);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return "$ " + new DecimalFormat("#,##0.00", symbols).format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    public static String date(LocalDate date) {
        return date == null ? "-" : date.format(DATE);
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE) + " " + value.format(TIME);
    }

    public static String time(LocalTime time) {
        return time == null ? "-" : time.format(TIME);
    }

    public static String average(Double value) {
        return value == null || value.isNaN() ? "-" : String.format(Locale.US, "%.2f", value);
    }

    public static String score(BigDecimal value) {
        return value == null ? "-" : value.setScale(1, RoundingMode.HALF_UP).toPlainString();
    }

    public static String orDash(Object value) {
        if (value == null) {
            return "-";
        }
        String text = value.toString();
        return text.isBlank() ? "-" : text;
    }

    public static String yesNo(boolean value) {
        return value ? "Sí" : "No";
    }

    public static String fullName(String first, String middle, String last1, String last2) {
        StringBuilder name = new StringBuilder(first);
        if (middle != null && !middle.isBlank()) {
            name.append(' ').append(middle);
        }
        name.append(' ').append(last1);
        if (last2 != null && !last2.isBlank()) {
            name.append(' ').append(last2);
        }
        return name.toString();
    }
}
