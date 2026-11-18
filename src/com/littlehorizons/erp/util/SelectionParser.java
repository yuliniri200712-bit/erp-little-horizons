package com.littlehorizons.erp.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/** Turns what the user types ("1,3" or "todos") into positions of a numbered list. */
public final class SelectionParser {

    public static final String ALL = "todos";

    private SelectionParser() {
    }

    /**
     * Positions from 1 to {@code size}, in the order typed, without repeating and at most {@code max};
     * "todos" selects the whole list. Invalid input throws {@link IllegalArgumentException} so the
     * console asks again.
     */
    public static List<Integer> parse(String text, int size, int max) {
        String clean = text == null ? "" : text.trim().toLowerCase();
        if (clean.equals(ALL)) {
            if (size > max) {
                throw new IllegalArgumentException("Se admiten máximo " + max + ": escriba los números que quiere incluir.");
            }
            return IntStream.rangeClosed(1, size).boxed().toList();
        }
        if (!clean.matches("\\d+(\\s*,\\s*\\d+)*")) {
            throw new IllegalArgumentException("Escriba los números separados por comas (ej: 1,3) o la palabra '" + ALL + "'.");
        }
        List<Integer> chosen = Arrays.stream(clean.split("\\s*,\\s*")).map(Integer::parseInt).toList();
        chosen.stream().filter(n -> n < 1 || n > size).findFirst().ifPresent(n -> {
            throw new IllegalArgumentException("El número " + n + " no está en la lista (1 a " + size + ").");
        });
        if (chosen.stream().distinct().count() != chosen.size()) {
            throw new IllegalArgumentException("Hay números repetidos.");
        }
        if (chosen.size() > max) {
            throw new IllegalArgumentException("Se admiten máximo " + max + ".");
        }
        return chosen;
    }
}
