package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.PaymentConcept;

import java.util.List;
import java.util.Optional;

/** CRUD for billing concepts (conceptos_pago). */
public interface PaymentConceptDao extends CrudDao<PaymentConcept> {

    Optional<PaymentConcept> findById(int id);

    Optional<PaymentConcept> findByCode(String code);

    List<PaymentConcept> findAll();

    int insert(PaymentConcept c);

    boolean update(PaymentConcept c);

    boolean delete(int id);
}
