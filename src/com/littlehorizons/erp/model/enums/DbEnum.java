package com.littlehorizons.erp.model.enums;

import java.util.Arrays;

/**
 * Contract for enums persisted in MySQL ENUM columns: {@link #label()} is the exact Spanish
 * value stored in the database and shown in the console.
 */
public interface DbEnum {

    String label();

    static <E extends Enum<E> & DbEnum> E fromLabel(Class<E> type, String label) {
        if (label == null) {
            return null;
        }
        return Arrays.stream(type.getEnumConstants())
                .filter(value -> value.label().equalsIgnoreCase(label.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Valor desconocido para " + type.getSimpleName() + ": " + label));
    }
}
