package com.littlehorizons.erp.view;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

/**
 * Reads keyboard lines so that accents and "ñ" arrive correctly whatever encoding the console uses.
 * Each line is decoded as UTF-8 (IntelliJ, modern terminals); if its bytes are not valid UTF-8 it is
 * decoded with the console's own encoding (Windows-1252 in IDE consoles, cp850 in a classic cmd window).
 */
final class ConsoleInput {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private final InputStream in;
    private final Charset fallback;

    ConsoleInput(InputStream in, Charset consoleCharset) {
        this.in = in;
        this.fallback = StandardCharsets.UTF_8.equals(consoleCharset) ? WINDOWS_1252 : consoleCharset;
    }

    /** Next line without the line break; {@link NoSuchElementException} when the input ends. */
    String nextLine() {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        try {
            int b;
            while ((b = in.read()) != -1 && b != '\n') {
                line.write(b);
            }
            if (b == -1 && line.size() == 0) {
                throw new NoSuchElementException("No line found");
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        byte[] bytes = line.toByteArray();
        int length = bytes.length > 0 && bytes[bytes.length - 1] == '\r' ? bytes.length - 1 : bytes.length;
        return decode(bytes, length);
    }

    private String decode(byte[] bytes, int length) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes, 0, length))
                    .toString();
        } catch (CharacterCodingException notUtf8) {
            return new String(bytes, 0, length, fallback);
        }
    }
}
