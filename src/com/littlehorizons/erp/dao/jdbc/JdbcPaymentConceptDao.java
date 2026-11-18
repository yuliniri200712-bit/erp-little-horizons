package com.littlehorizons.erp.dao.jdbc;

import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.model.PaymentConcept;

import java.util.List;
import java.util.Optional;

/** JDBC implementation of {@link PaymentConceptDao}: CRUD for billing concepts (conceptos_pago). */
public class JdbcPaymentConceptDao extends BaseDao implements PaymentConceptDao {

    private static final RowMapper<PaymentConcept> MAPPER = rs -> new PaymentConcept(
            rs.getInt("id_concepto"), rs.getString("codigo_concepto"), rs.getString("nombre_concepto"),
            rs.getString("descripcion"), rs.getBigDecimal("monto_base"), rs.getBigDecimal("porcentaje_iva"),
            rs.getBoolean("es_recurrente"), rs.getBoolean("esta_activo"));

    @Override
    public Optional<PaymentConcept> findById(int id) {
        return queryOne("SELECT * FROM conceptos_pago WHERE id_concepto = ?", MAPPER, id);
    }

    @Override
    public Optional<PaymentConcept> findByCode(String code) {
        return queryOne("SELECT * FROM conceptos_pago WHERE codigo_concepto = ?", MAPPER, code);
    }

    @Override
    public List<PaymentConcept> findAll() {
        return query("SELECT * FROM conceptos_pago ORDER BY id_concepto", MAPPER);
    }

    @Override
    public int insert(PaymentConcept c) {
        return insert("INSERT INTO conceptos_pago (codigo_concepto, nombre_concepto, descripcion, monto_base, porcentaje_iva, "
                        + "es_recurrente, esta_activo) VALUES (?, ?, ?, ?, ?, ?, ?)", c.code(), c.name(), c.description(),
                c.baseAmount(), c.taxRate(), c.recurring(), c.active());
    }

    @Override
    public boolean update(PaymentConcept c) {
        return update("UPDATE conceptos_pago SET codigo_concepto = ?, nombre_concepto = ?, descripcion = ?, monto_base = ?, "
                        + "porcentaje_iva = ?, es_recurrente = ?, esta_activo = ? WHERE id_concepto = ?", c.code(), c.name(),
                c.description(), c.baseAmount(), c.taxRate(), c.recurring(), c.active(), c.id()) > 0;
    }

    @Override
    public boolean delete(int id) {
        return update("DELETE FROM conceptos_pago WHERE id_concepto = ?", id) > 0;
    }
}
