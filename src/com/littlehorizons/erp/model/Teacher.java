package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.TeacherStatus;
import com.littlehorizons.erp.util.Formatter;

import java.time.LocalDate;

/** Teacher directory entry. Table: profesores. */
public record Teacher(
        Integer id,
        DocumentType documentType,
        String documentNumber,
        String firstName,
        String middleName,
        String lastName,
        String secondLastName,
        String email,
        String phone,
        String specialization,
        LocalDate hireDate,
        TeacherStatus status) {

    public String fullName() {
        return Formatter.fullName(firstName, middleName, lastName, secondLastName);
    }
}
