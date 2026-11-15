package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.enums.BorrowerType;
import com.littlehorizons.erp.model.enums.LoanStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Loans to students and teachers. The SQL is built from {@link BorrowerType}, so the same code
 * works on prestamos_estudiante and prestamos_profesor (Open/Closed: a new borrower type only
 * needs a new enum constant). Table and column names come from the enum, never from user input.
 */
public class LoanDao extends BaseDao {

    private static String select(BorrowerType type) {
        return "SELECT l.*, l." + type.borrowerColumn() + " AS id_solicitante, r.codigo_recurso, r.nombre_recurso, "
                + fullNameSql("x") + " AS nombre_solicitante FROM " + type.loanTable() + " l "
                + "JOIN recursos r ON r.id_recurso = l.id_recurso "
                + "JOIN " + type.personTable() + " x ON x." + type.personKey() + " = l." + type.borrowerColumn() + " ";
    }

    private static RowMapper<Loan> mapper(BorrowerType type) {
        return rs -> new Loan(rs.getInt("id_prestamo"), type, rs.getInt("id_recurso"), rs.getString("codigo_recurso"),
                rs.getString("nombre_recurso"), rs.getInt("id_solicitante"), rs.getString("nombre_solicitante"),
                localDate(rs, "fecha_prestamo"), localDate(rs, "fecha_devolucion_esperada"), localDate(rs, "fecha_devolucion_real"),
                enumValue(rs, "estado_prestamo", LoanStatus.class), rs.getString("observaciones"));
    }

    public int insert(Loan loan) {
        BorrowerType type = loan.borrowerType();
        return insert("INSERT INTO " + type.loanTable() + " (id_recurso, " + type.borrowerColumn() + ", fecha_prestamo, "
                        + "fecha_devolucion_esperada, estado_prestamo, observaciones) VALUES (?, ?, ?, ?, 'Activo', ?)",
                loan.resourceId(), loan.borrowerId(), loan.loanDate(), loan.expectedReturnDate(), loan.notes());
    }

    public Optional<Loan> findById(BorrowerType type, int id) {
        return queryOne(select(type) + "WHERE l.id_prestamo = ?", mapper(type), id);
    }

    public List<Loan> findOpen(BorrowerType type) {
        return query(select(type) + "WHERE l.estado_prestamo IN ('Activo', 'Vencido') ORDER BY l.fecha_devolucion_esperada", mapper(type));
    }

    public List<Loan> findOverdue(BorrowerType type) {
        return query(select(type) + "WHERE l.estado_prestamo = 'Vencido' ORDER BY l.fecha_devolucion_esperada", mapper(type));
    }

    public List<Loan> findByBorrower(BorrowerType type, int borrowerId) {
        return query(select(type) + "WHERE l." + type.borrowerColumn() + " = ? ORDER BY l.fecha_prestamo DESC", mapper(type), borrowerId);
    }

    public List<Loan> findByResource(BorrowerType type, int resourceId) {
        return query(select(type) + "WHERE l.id_recurso = ? ORDER BY l.fecha_prestamo DESC", mapper(type), resourceId);
    }

    public boolean markReturned(BorrowerType type, int loanId, LocalDate returnDate, String notes) {
        return update("UPDATE " + type.loanTable() + " SET estado_prestamo = 'Devuelto', fecha_devolucion_real = ?, "
                + "observaciones = COALESCE(?, observaciones) WHERE id_prestamo = ? AND estado_prestamo IN ('Activo', 'Vencido')",
                returnDate, notes, loanId) > 0;
    }

    /** Marks as 'Vencido' every active loan whose expected return date has passed. */
    public int refreshOverdue(BorrowerType type) {
        return update("UPDATE " + type.loanTable() + " SET estado_prestamo = 'Vencido' "
                + "WHERE estado_prestamo = 'Activo' AND fecha_devolucion_esperada < CURDATE()");
    }

    public long countOpenLoans(int resourceId) {
        return count("SELECT (SELECT COUNT(*) FROM prestamos_estudiante WHERE id_recurso = ? AND estado_prestamo IN ('Activo', 'Vencido')) "
                + "+ (SELECT COUNT(*) FROM prestamos_profesor WHERE id_recurso = ? AND estado_prestamo IN ('Activo', 'Vencido'))",
                resourceId, resourceId);
    }
}
