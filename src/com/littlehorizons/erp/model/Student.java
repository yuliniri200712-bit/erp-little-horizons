package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.model.enums.Gender;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.util.Formatter;

import java.time.LocalDate;
import java.time.Period;

/** Student personal data. Table: estudiantes. */
public record Student(
        Integer id,
        DocumentType documentType,
        String documentNumber,
        String firstName,
        String middleName,
        String lastName,
        String secondLastName,
        LocalDate birthDate,
        Gender gender,
        String address,
        String phone,
        String email,
        String bloodType,
        String allergies,
        String medicalConditions,
        LocalDate admissionDate,
        StudentStatus status) {

    public String fullName() {
        return Formatter.fullName(firstName, middleName, lastName, secondLastName);
    }

    public int age() {
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public Student withId(int newId) {
        return new Student(newId, documentType, documentNumber, firstName, middleName, lastName, secondLastName,
                birthDate, gender, address, phone, email, bloodType, allergies, medicalConditions, admissionDate, status);
    }
}
