package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.DocumentType;
import com.littlehorizons.erp.util.Formatter;

/** Guardian (acudiente) with contact data. Table: acudientes. */
public record Guardian(
        Integer id,
        DocumentType documentType,
        String documentNumber,
        String firstName,
        String middleName,
        String lastName,
        String secondLastName,
        String relationship,
        String mainPhone,
        String secondaryPhone,
        String email,
        String address,
        String occupation,
        boolean financialResponsible) {

    public String fullName() {
        return Formatter.fullName(firstName, middleName, lastName, secondLastName);
    }

    public Guardian withId(int newId) {
        return new Guardian(newId, documentType, documentNumber, firstName, middleName, lastName, secondLastName,
                relationship, mainPhone, secondaryPhone, email, address, occupation, financialResponsible);
    }
}
