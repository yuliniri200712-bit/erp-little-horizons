package com.littlehorizons.erp.exception;

import java.sql.SQLException;

/**
 * Unchecked wrapper for {@link SQLException}. Translates MySQL error codes into
 * user friendly Spanish messages while keeping the technical cause for the error log.
 */
public class DataAccessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final int DUPLICATE_ENTRY = 1062;
    private static final int ROW_IS_REFERENCED = 1451;
    private static final int ROW_IS_REFERENCED_2 = 1217;
    private static final int NO_REFERENCED_ROW = 1452;
    private static final int NO_REFERENCED_ROW_2 = 1216;
    private static final int CHECK_VIOLATED = 3819;
    private static final int DATA_TOO_LONG = 1406;
    private static final int OUT_OF_RANGE = 1264;
    private static final int TRUNCATED = 1265;
    private static final int INCORRECT_VALUE = 1366;
    private static final int BAD_NULL = 1048;
    private static final int ACCESS_DENIED = 1045;
    private static final int UNKNOWN_DATABASE = 1049;

    private final String userMessage;

    public DataAccessException(String context, SQLException cause) {
        super(context + ": " + cause.getMessage(), cause);
        this.userMessage = translate(cause);
    }

    public String getUserMessage() {
        return userMessage;
    }

    public boolean isConnectionProblem() {
        SQLException sql = (SQLException) getCause();
        String state = sql.getSQLState();
        return (state != null && state.startsWith("08")) || sql.getErrorCode() == ACCESS_DENIED
                || sql.getErrorCode() == UNKNOWN_DATABASE;
    }

    private static String translate(SQLException e) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("08")) {
            return "No hay conexión con la base de datos. Verifique que el contenedor MySQL esté en ejecución.";
        }
        return switch (e.getErrorCode()) {
            case DUPLICATE_ENTRY -> "Ya existe un registro con ese valor único (" + extractDuplicate(e.getMessage()) + ").";
            case ROW_IS_REFERENCED, ROW_IS_REFERENCED_2 ->
                    "No se puede eliminar o modificar: existen registros relacionados que dependen de él.";
            case NO_REFERENCED_ROW, NO_REFERENCED_ROW_2 -> "Referencia inválida: el registro relacionado no existe.";
            case CHECK_VIOLATED -> "El dato no cumple una restricción de la base de datos (CHECK).";
            case DATA_TOO_LONG -> "Uno de los valores excede la longitud permitida.";
            case OUT_OF_RANGE, TRUNCATED, INCORRECT_VALUE -> "Uno de los valores está fuera del rango o formato permitido.";
            case BAD_NULL -> "Falta un dato obligatorio.";
            case ACCESS_DENIED -> "Acceso denegado a MySQL: revise usuario y contraseña en config/db.properties.";
            case UNKNOWN_DATABASE -> "La base de datos erp_little_horizons no existe. Ejecute docs/database.sql.";
            default -> "Error de base de datos (código " + e.getErrorCode() + "). Consulte error_log.txt.";
        };
    }

    private static String extractDuplicate(String message) {
        if (message == null) {
            return "duplicado";
        }
        int start = message.indexOf('\'');
        int end = start >= 0 ? message.indexOf('\'', start + 1) : -1;
        return start >= 0 && end > start ? "valor " + message.substring(start, end + 1) : "duplicado";
    }
}
