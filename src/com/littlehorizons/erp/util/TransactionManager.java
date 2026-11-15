package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Executes a unit of work inside a single JDBC transaction (commit on success,
 * rollback on any exception). Nested calls join the outer transaction.
 */
public final class TransactionManager {

    private TransactionManager() {
    }

    public static <T> T inTransaction(Supplier<T> work) {
        DatabaseConnection database = DatabaseConnection.getInstance();
        if (database.hasTransaction()) {
            return work.get();
        }
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            database.bind(connection);
            try {
                T result = work.get();
                connection.commit();
                return result;
            } catch (RuntimeException e) {
                rollbackQuietly(connection);
                throw e;
            } finally {
                database.unbind();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Transaction", e);
        }
    }

    public static void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }

    private static void rollbackQuietly(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException e) {
            ErrorLogger.error("Transaction rollback", e);
        }
    }
}
