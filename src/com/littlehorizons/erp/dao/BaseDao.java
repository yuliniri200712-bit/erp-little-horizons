package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.enums.DbEnum;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.util.DatabaseConnection.ConnectionHandle;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Template for every JDBC DAO: all statements are {@link PreparedStatement}s executed inside
 * try-with-resources, parameters are bound in one place and {@link SQLException}s are
 * translated into {@link DataAccessException}. Subclasses only provide SQL and a row mapper.
 */
public abstract class BaseDao {

    /** Maps the current row of a {@link ResultSet} to an object (used with lambdas). */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }

    protected DatabaseConnection database() {
        return DatabaseConnection.getInstance();
    }

    protected <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        try (ConnectionHandle handle = database().acquire();
             PreparedStatement statement = handle.connection().prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                while (resultSet.next()) {
                    rows.add(mapper.map(resultSet));
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new DataAccessException(getClass().getSimpleName() + ".query", e);
        }
    }

    protected <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    /** Executes INSERT/UPDATE/DELETE and returns the affected rows. */
    protected int update(String sql, Object... params) {
        try (ConnectionHandle handle = database().acquire();
             PreparedStatement statement = handle.connection().prepareStatement(sql)) {
            bind(statement, params);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(getClass().getSimpleName() + ".update", e);
        }
    }

    /** Executes an INSERT and returns the generated AUTO_INCREMENT key. */
    protected int insert(String sql, Object... params) {
        try (ConnectionHandle handle = database().acquire();
             PreparedStatement statement = handle.connection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, params);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException(getClass().getSimpleName() + ".insert", e);
        }
    }

    protected long count(String sql, Object... params) {
        return queryOne(sql, rs -> rs.getLong(1), params).orElse(0L);
    }

    protected BigDecimal sum(String sql, Object... params) {
        return queryOne(sql, rs -> rs.getBigDecimal(1), params).filter(v -> v != null).orElse(BigDecimal.ZERO);
    }

    private static void bind(PreparedStatement statement, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            int index = i + 1;
            switch (value) {
                case null -> statement.setNull(index, Types.NULL);
                case DbEnum dbEnum -> statement.setString(index, dbEnum.label());
                case LocalDate date -> statement.setObject(index, date);
                case LocalTime time -> statement.setObject(index, time);
                case Boolean bool -> statement.setBoolean(index, bool);
                default -> statement.setObject(index, value);
            }
        }
    }

    // ---------------------------------------------------------------- mapping helpers

    protected static LocalDate localDate(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalDate.class);
    }

    protected static LocalTime localTime(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, LocalTime.class);
    }

    protected static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    protected static <E extends Enum<E> & DbEnum> E enumValue(ResultSet rs, String column, Class<E> type)
            throws SQLException {
        return DbEnum.fromLabel(type, rs.getString(column));
    }

    /** SQL expression with the full name of a person table alias (NULL parts are skipped). */
    protected static String fullNameSql(String alias) {
        return "CONCAT_WS(' ', " + alias + ".primer_nombre, " + alias + ".segundo_nombre, "
                + alias + ".primer_apellido, " + alias + ".segundo_apellido)";
    }

    /** Wildcard pattern for LIKE searches. */
    protected static String like(String text) {
        return "%" + text.trim() + "%";
    }
}
