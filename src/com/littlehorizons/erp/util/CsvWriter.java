package com.littlehorizons.erp.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Writes tabular data to CSV files (UTF-8 with BOM and ';' separator so Excel in Spanish opens
 * accents and columns correctly). Uses try-with-resources and a buffered writer.
 */
public final class CsvWriter {

    private static final char SEPARATOR = ';';
    private static final String BOM = "﻿";

    private CsvWriter() {
    }

    /**
     * Writes {@code items} to {@code file}; each item is converted to a row by {@code toRow}.
     *
     * @return number of data rows written
     */
    public static <T> int write(Path file, List<String> headers, List<T> items, Function<T, List<?>> toRow) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(BOM);
            writer.write(line(headers));
            writer.newLine();
            for (T item : items) {
                writer.write(line(toRow.apply(item)));
                writer.newLine();
            }
        }
        return items.size();
    }

    private static String line(List<?> values) {
        return values.stream().map(CsvWriter::escape).collect(Collectors.joining(String.valueOf(SEPARATOR)));
    }

    /** Quotes values containing separators, quotes or line breaks (RFC 4180 style). */
    private static String escape(Object value) {
        String text = value == null ? "" : value.toString();
        boolean needsQuotes = text.indexOf(SEPARATOR) >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0;
        return needsQuotes ? '"' + text.replace("\"", "\"\"") + '"' : text;
    }
}
