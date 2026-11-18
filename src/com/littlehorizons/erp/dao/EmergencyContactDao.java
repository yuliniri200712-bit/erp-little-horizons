package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.EmergencyContact;

import java.util.List;
import java.util.Optional;

/** Additional emergency contacts of students (contactos_emergencia). */
public interface EmergencyContactDao {

    int insert(EmergencyContact c);

    Optional<EmergencyContact> findById(int id);

    List<EmergencyContact> findByStudent(int studentId);

    boolean delete(int id);
}
