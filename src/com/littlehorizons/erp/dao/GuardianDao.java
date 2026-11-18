package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.GuardianLink;
import com.littlehorizons.erp.model.Student;

import java.util.List;
import java.util.Optional;

/** CRUD for guardians (acudientes) and the student-guardian relationship (estudiante_acudiente). */
public interface GuardianDao extends CrudDao<Guardian> {

    Optional<Guardian> findById(int id);

    Optional<Guardian> findByDocument(String documentNumber);

    List<Guardian> findAll();

    List<Guardian> search(String text);

    int insert(Guardian g);

    boolean update(Guardian g);

    boolean delete(int id);

    void link(int studentId, int guardianId, boolean primary, boolean emergencyContact);

    void clearPrimary(int studentId);

    long countPayments(int guardianId);

    boolean unlink(int studentId, int guardianId);

    boolean isLinked(int studentId, int guardianId);

    long countGuardiansOf(int studentId);

    /** Number of students whose only guardian is the given one. */
    long countStudentsWithOnlyThisGuardian(int guardianId);

    List<GuardianLink> findByStudent(int studentId);

    List<Student> findStudentsOf(int guardianId);

    /** Guardian responsible for payments: primary and financially responsible first. */
    Optional<Guardian> findFinancialGuardian(int studentId);
}
