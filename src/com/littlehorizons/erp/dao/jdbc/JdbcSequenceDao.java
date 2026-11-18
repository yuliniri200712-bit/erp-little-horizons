package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.SequenceDao;
import com.littlehorizons.erp.exception.BusinessException;

/** JDBC implementation of {@link SequenceDao}: * Document numbering (consecutivos). {@link #next(String)} must run inside a transaction:
 * the row is locked with SELECT ... FOR UPDATE so two payments never get the same number. */
public class JdbcSequenceDao extends BaseDao implements SequenceDao {


    private record SequenceRow(String prefix, int last) {
    }

    /** Reserves and returns the next number formatted as PREFIX-000123. */
    @Override
    public String next(String type) {
        SequenceRow row = queryOne("SELECT prefijo, ultimo_numero FROM consecutivos WHERE tipo_documento = ? FOR UPDATE",
                rs -> new SequenceRow(rs.getString("prefijo"), rs.getInt("ultimo_numero")), type)
                .orElseThrow(() -> new BusinessException("No existe el consecutivo " + type + " en la tabla consecutivos."));
        int next = row.last() + 1;
        update("UPDATE consecutivos SET ultimo_numero = ? WHERE tipo_documento = ?", next, type);
        return String.format("%s-%06d", row.prefix(), next);
    }
}
