package com.littlehorizons.erp.dao;


/**
 * Document numbering (consecutivos). {@link #next(String)} must run inside a transaction:
 * the row is locked with SELECT ... FOR UPDATE so two payments never get the same number.
 */
public interface SequenceDao {

    String RECEIPT = "RECIBO";
    String INVOICE = "FACTURA";

    /** Reserves and returns the next number formatted as PREFIX-000123. */
    String next(String type);
}
