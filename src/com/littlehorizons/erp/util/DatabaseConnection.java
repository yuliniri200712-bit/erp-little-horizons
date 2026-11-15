package com.littlehorizons.erp.util;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.exception.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton that provides JDBC connections to MySQL. When a transaction is open on the
 * current thread (see {@link TransactionManager}) the same connection is reused, so DAO
 * calls inside the transaction share it and do not close it.
 */
public final class DatabaseConnection {

    private static volatile DatabaseConnection instance;

    private final AppConfig config;
    private final ThreadLocal<Connection> transactionalConnection = new ThreadLocal<>();

    private DatabaseConnection(AppConfig config) {
        this.config = config;
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection(AppConfig.getInstance());
                }
            }
        }
        return instance;
    }

    /** Opens a brand-new physical connection; the caller is responsible for closing it. */
    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(config.dbUrl(), config.dbUser(), config.dbPassword());
    }

    /** Returns a handle that closes the connection only if it is not part of a transaction. */
    public ConnectionHandle acquire() throws SQLException {
        Connection current = transactionalConnection.get();
        if (current != null) {
            return new ConnectionHandle(current, false);
        }
        return new ConnectionHandle(openConnection(), true);
    }

    boolean hasTransaction() {
        return transactionalConnection.get() != null;
    }

    void bind(Connection connection) {
        transactionalConnection.set(connection);
    }

    void unbind() {
        transactionalConnection.remove();
    }

    /** Verifies connectivity and returns the MySQL server version. */
    public String testConnection() {
        try (Connection connection = openConnection()) {
            return connection.getMetaData().getDatabaseProductVersion();
        } catch (SQLException e) {
            throw new DataAccessException("Connection test", e);
        }
    }

    /** Connection wrapper used with try-with-resources. */
    public static final class ConnectionHandle implements AutoCloseable {
        private final Connection connection;
        private final boolean owned;

        private ConnectionHandle(Connection connection, boolean owned) {
            this.connection = connection;
            this.owned = owned;
        }

        public Connection connection() {
            return connection;
        }

        @Override
        public void close() throws SQLException {
            if (owned) {
                connection.close();
            }
        }
    }
}
