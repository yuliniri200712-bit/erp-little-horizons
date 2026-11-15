package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.ResourceStatus;
import java.time.LocalDate;

/** Inventory resource (book, lab equipment, sports equipment, device). Table: recursos. */
public record Resource(
        Integer id,
        String code,
        String name,
        int categoryId,
        String categoryName,
        String description,
        LocalDate acquisitionDate,
        String location,
        ResourceStatus status) {
}
