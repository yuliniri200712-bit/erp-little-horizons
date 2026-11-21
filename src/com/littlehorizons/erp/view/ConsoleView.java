package com.littlehorizons.erp.view;

import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.exception.MainMenuRequestedException;
import com.littlehorizons.erp.model.enums.DbEnum;
import com.littlehorizons.erp.util.Formatter;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Low-level console I/O shared by every view: headers, messages, tables, menus and typed input
 * with validation and retry. No business logic or SQL lives here.
 */
public class ConsoleView {

    /** Typed at any prompt, returns straight to the main menu. */
    public static final String HOME_KEYWORD = "inicio";
    private static final int MAX_COLUMN_WIDTH = 42;
    private static final int LINE_WIDTH = 78;
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("d/M/yyyy"), DateTimeFormatter.ISO_LOCAL_DATE);

    private final ConsoleInput input;
    private final PrintStream out;

    public ConsoleView() {
        this.input = new ConsoleInput(System.in, inputCharset());
        this.out = System.out;
    }

    /**
     * Console encoding used when a typed line is not valid UTF-8 (see {@link ConsoleInput}): the
     * {@code lh.console.encoding} property if given; the real console encoding in a terminal; otherwise
     * (IDE consoles such as IntelliJ) the same encoding used for standard output.
     */
    private static Charset inputCharset() {
        String forced = System.getProperty("lh.console.encoding");
        String encoding = forced != null ? forced
                : System.console() != null ? System.getProperty("stdin.encoding")
                : System.getProperty("sun.stdout.encoding",
                System.getProperty("stdout.encoding", System.getProperty("stdin.encoding")));
        try {
            return encoding == null ? Charset.defaultCharset() : Charset.forName(encoding);
        } catch (IllegalArgumentException e) {
            return Charset.defaultCharset();
        }
    }

    // ================================================================ output

    public void header(String title) {
        out.println();
        out.println("=".repeat(LINE_WIDTH));
        out.println("  " + title);
        out.println("=".repeat(LINE_WIDTH));
    }

    public void section(String title) {
        out.println();
        out.println("--- " + title + " " + "-".repeat(Math.max(3, LINE_WIDTH - title.length() - 5)));
    }

    public void info(String message) {
        out.println(message);
    }

    public void success(String message) {
        out.println("[OK] " + message);
    }

    public void warning(String message) {
        out.println("[!] " + message);
    }

    public void error(String message) {
        out.println("[ERROR] " + message);
    }

    public void field(String label, Object value) {
        out.printf("  %-26s %s%n", label + ":", Formatter.orDash(value));
    }

    /** Prints an aligned text table; long cells are truncated. */
    public void table(List<String> headers, List<List<String>> rows) {
        if (rows.isEmpty()) {
            info("(sin registros)");
            return;
        }
        int[] widths = IntStream.range(0, headers.size())
                .map(col -> Math.min(MAX_COLUMN_WIDTH, Math.max(headers.get(col).length(),
                        rows.stream().mapToInt(r -> cell(r, col).length()).max().orElse(0))))
                .toArray();
        String separator = Arrays.stream(widths).mapToObj(w -> "-".repeat(w + 2)).collect(Collectors.joining("+", "+", "+"));
        out.println(separator);
        out.println(formatRow(headers, widths));
        out.println(separator);
        rows.forEach(row -> out.println(formatRow(row, widths)));
        out.println(separator);
        info("Total: " + rows.size() + " registro(s)");
    }

    private static String cell(List<String> row, int col) {
        return col < row.size() && row.get(col) != null ? row.get(col) : "-";
    }

    private static String formatRow(List<String> row, int[] widths) {
        StringBuilder line = new StringBuilder("|");
        for (int col = 0; col < widths.length; col++) {
            String value = cell(row, col);
            if (value.length() > widths[col]) {
                value = value.substring(0, widths[col] - 1) + "…";
            }
            line.append(' ').append(String.format("%-" + widths[col] + "s", value)).append(" |");
        }
        return line.toString();
    }

    public void pause() {
        out.print("\nPresione Enter para continuar...");
        nextLine();
    }

    // ================================================================ menus

    /** Shows a numbered menu with "0. Volver/Salir" and returns the chosen option. */
    public int menu(String title, List<String> options, String exitLabel) {
        header(title);
        for (int i = 0; i < options.size(); i++) {
            out.printf("  %2d. %s%n", i + 1, options.get(i));
        }
        out.printf("  %2d. %s%n", 0, exitLabel);
        out.println("  (Escriba '" + HOME_KEYWORD + "' en cualquier momento para ir al menú principal)");
        return readInt("Seleccione una opción", 0, options.size());
    }

    /** Boxed banner used by the main menu (same style as the specification's example). */
    public void banner(String title, String subtitle) {
        int width = Math.max(title.length(), subtitle.length()) + 6;
        out.println();
        out.println("╔" + "═".repeat(width) + "╗");
        out.println("║" + padRight("   " + title, width) + "║");
        out.println("║" + padRight("   " + subtitle, width) + "║");
        out.println("╚" + "═".repeat(width) + "╝");
        out.println();
    }

    private static String padRight(String text, int width) {
        return text.length() >= width ? text : text + " ".repeat(width - text.length());
    }

    // ================================================================ input

    private String nextLine() {
        String line = input.nextLine().strip(); // NoSuchElementException when input ends (handled in Main)
        if (HOME_KEYWORD.equalsIgnoreCase(line)) {
            throw new MainMenuRequestedException();
        }
        return line;
    }

    public String readLine(String prompt) {
        out.print(prompt + ": ");
        return nextLine();
    }

    public String readRequired(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isBlank()) {
                return value;
            }
            error("Este dato es obligatorio.");
        }
    }

    /** Optional text; Enter keeps {@code current} (shown in brackets). "-" clears the value. */
    public String readOptional(String prompt, String current) {
        String value = readLine(prompt + (current == null ? " (opcional)" : " [" + current + "]"));
        if (value.isBlank()) {
            return current;
        }
        return "-".equals(value) ? null : value;
    }

    /** Required text with a default value kept on Enter. */
    public String readText(String prompt, String current) {
        if (current == null) {
            return readRequired(prompt);
        }
        String value = readLine(prompt + " [" + current + "]");
        return value.isBlank() ? current : value;
    }

    public int readInt(String prompt, int min, int max) {
        return readValidated(prompt, text -> parseInt(text, min, max));
    }

    public int readInt(String prompt, int min, int max, Integer current) {
        if (current == null) {
            return readInt(prompt, min, max);
        }
        return readValidated(prompt + " [" + current + "]", text -> text.isBlank() ? current : parseInt(text, min, max));
    }

    /** Optional integer: Enter returns {@code null}. */
    public Integer readOptionalInt(String prompt, int min, int max) {
        return readValidated(prompt + " (Enter = todos)", text -> text.isBlank() ? null : parseInt(text, min, max));
    }

    private static int parseInt(String text, int min, int max) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value < min || value > max) {
                throw new BusinessException("Ingrese un número entre " + min + " y " + max + ".");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new BusinessException("Ingrese un número entero válido.");
        }
    }

    public LocalDate readDate(String prompt, LocalDate current) {
        String suffix = current == null ? " (dd/mm/aaaa)" : " (dd/mm/aaaa) [" + Formatter.date(current) + "]";
        return readValidated(prompt + suffix, text -> {
            if (text.isBlank()) {
                if (current == null) {
                    throw new BusinessException("La fecha es obligatoria.");
                }
                return current;
            }
            return parseDate(text);
        });
    }

    public LocalDate readOptionalDate(String prompt) {
        return readValidated(prompt + " (dd/mm/aaaa, opcional)", text -> text.isBlank() ? null : parseDate(text));
    }

    private static LocalDate parseDate(String text) {
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(text.trim(), format);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        throw new BusinessException("Fecha inválida. Use el formato dd/mm/aaaa (ejemplo: 15/11/2026).");
    }

    public LocalTime readTime(String prompt) {
        return readValidated(prompt + " (HH:mm)", text -> {
            try {
                return LocalTime.parse(text.trim().length() == 4 ? "0" + text.trim() : text.trim(), Formatter.TIME);
            } catch (DateTimeParseException e) {
                throw new BusinessException("Hora inválida. Use el formato HH:mm (ejemplo: 07:30).");
            }
        });
    }

    /** Reads an amount in pesos; accepts 350000, 350.000 or 350000,50. */
    public BigDecimal readMoney(String prompt, BigDecimal current) {
        String suffix = current == null ? "" : " [" + Formatter.money(current) + "]";
        return readValidated(prompt + suffix, text -> {
            if (text.isBlank() && current != null) {
                return current;
            }
            return parseMoney(text);
        });
    }

    private static BigDecimal parseMoney(String text) {
        String cleaned = text.replace("$", "").replace(" ", "");
        if (cleaned.matches("\\d{1,3}(\\.\\d{3})+(,\\d{1,2})?")) {
            cleaned = cleaned.replace(".", "").replace(',', '.');
        } else {
            cleaned = cleaned.replace(',', '.');
        }
        try {
            BigDecimal value = new BigDecimal(cleaned);
            if (value.signum() < 0) {
                throw new BusinessException("El valor no puede ser negativo.");
            }
            return value;
        } catch (NumberFormatException e) {
            throw new BusinessException("Valor inválido. Ejemplos válidos: 350000, 350.000 o 350000,50.");
        }
    }

    public BigDecimal readDecimal(String prompt, BigDecimal current) {
        String suffix = current == null ? "" : " [" + current.stripTrailingZeros().toPlainString() + "]";
        return readValidated(prompt + suffix, text -> {
            if (text.isBlank() && current != null) {
                return current;
            }
            try {
                return new BigDecimal(text.trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                throw new BusinessException("Número decimal inválido.");
            }
        });
    }

    public boolean confirm(String prompt) {
        return readValidated(prompt + " (s/n)", text -> switch (text.toLowerCase()) {
            case "s", "si", "sí", "y", "yes" -> true;
            case "n", "no" -> false;
            default -> throw new BusinessException("Responda 's' o 'n'.");
        });
    }

    public boolean readBoolean(String prompt, boolean current) {
        return readValidated(prompt + " (s/n) [" + (current ? "s" : "n") + "]", text -> switch (text.toLowerCase()) {
            case "" -> current;
            case "s", "si", "sí" -> true;
            case "n", "no" -> false;
            default -> throw new BusinessException("Responda 's' o 'n'.");
        });
    }

    /** Lets the user pick an enum constant from a numbered list. */
    public <E extends Enum<E> & DbEnum> E chooseEnum(String prompt, E[] values, E current) {
        List<String> labels = Arrays.stream(values).map(DbEnum::label).toList();
        for (int i = 0; i < labels.size(); i++) {
            out.printf("    %d) %s%n", i + 1, labels.get(i));
        }
        Integer currentIndex = current == null ? null : Arrays.asList(values).indexOf(current) + 1;
        int choice = readInt(prompt, 1, values.length, currentIndex);
        return values[choice - 1];
    }

    /** Optional enum choice: Enter returns {@code null}. */
    public <E extends Enum<E> & DbEnum> E chooseOptionalEnum(String prompt, E[] values) {
        for (int i = 0; i < values.length; i++) {
            out.printf("    %d) %s%n", i + 1, values[i].label());
        }
        Integer choice = readOptionalInt(prompt, 1, values.length);
        return choice == null ? null : values[choice - 1];
    }

    /** Lets the user pick one element of a list (numbered). */
    public <T> T chooseFrom(String prompt, List<T> items, Function<T, String> label) {
        if (items.isEmpty()) {
            throw new BusinessException("No hay opciones disponibles.");
        }
        for (int i = 0; i < items.size(); i++) {
            out.printf("    %d) %s%n", i + 1, label.apply(items.get(i)));
        }
        return items.get(readInt(prompt, 1, items.size()) - 1);
    }

    /**
     * Reads a value until {@code parser} accepts it. Parsers signal invalid input with
     * {@link BusinessException} or {@link IllegalArgumentException}; the message is shown and the
     * question is repeated (retry on invalid input).
     */
    public <T> T readValidated(String prompt, Function<String, T> parser) {
        while (true) {
            String text = readLine(prompt);
            try {
                return parser.apply(text);
            } catch (BusinessException | IllegalArgumentException e) {
                error(e.getMessage());
            }
        }
    }

    /** Builds a table row from values, converting nulls to "-". */
    public static List<String> row(Object... values) {
        List<String> cells = new ArrayList<>(values.length);
        for (Object value : values) {
            cells.add(Formatter.orDash(value));
        }
        return cells;
    }
}
