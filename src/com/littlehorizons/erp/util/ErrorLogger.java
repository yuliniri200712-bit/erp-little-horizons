package com.littlehorizons.erp.util;

import com.littlehorizons.erp.config.AppConfig;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Appends technical errors to {@code error_log.txt} (path configurable through
 * {@code log.file} in config/app.properties). Each entry has timestamp, level, context,
 * message and stack trace. Business validations are not logged, only technical failures.
 */
public final class ErrorLogger {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private ErrorLogger() {
    }

    public static void error(String context, Throwable error) {
        StringWriter trace = new StringWriter();
        error.printStackTrace(new PrintWriter(trace));
        write("ERROR", context, error.getClass().getSimpleName() + ": " + error.getMessage()
                + System.lineSeparator() + trace);
    }

    public static void warn(String context, String message) {
        write("WARN", context, message + System.lineSeparator());
    }

    private static synchronized void write(String level, String context, String detail) {
        Path file = AppConfig.getInstance().logFile();
        String entry = "[" + LocalDateTime.now().format(TIMESTAMP) + "] [" + level + "] [" + context + "] "
                + detail + "----" + System.lineSeparator();
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, entry, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("No se pudo escribir en el log de errores: " + e.getMessage());
        }
    }
}
