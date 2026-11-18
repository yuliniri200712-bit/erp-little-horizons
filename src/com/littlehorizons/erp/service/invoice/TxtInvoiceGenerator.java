package com.littlehorizons.erp.service.invoice;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.report.InvoiceData;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Saves the invoice as a UTF-8 text file (factura_000123.txt) with the lines of
 * {@link InvoiceTextRenderer}: the same text that is printed in the console.
 */
public class TxtInvoiceGenerator implements InvoiceGenerator {

    private final AppConfig config;

    public TxtInvoiceGenerator(AppConfig config) {
        this.config = config;
    }

    @Override
    public String format() {
        return "TXT";
    }

    @Override
    public Path pathFor(String invoiceNumber) {
        return config.invoiceOutputDir().toAbsolutePath().resolve(Invoice.baseFileName(invoiceNumber) + ".txt");
    }

    @Override
    public Path generate(InvoiceData data) throws IOException {
        Path file = pathFor(data.invoice().number());
        Files.createDirectories(file.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            for (String line : InvoiceTextRenderer.render(data)) {
                writer.write(line);
                writer.newLine();
            }
        }
        return file;
    }
}
