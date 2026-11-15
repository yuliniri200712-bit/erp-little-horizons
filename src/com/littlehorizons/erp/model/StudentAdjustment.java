package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.AdjustmentType;
import java.math.BigDecimal;

/** Scholarship or discount of a student on a concept. Table: ajustes_estudiante. */
public record StudentAdjustment(
        Integer id,
        int studentId,
        int conceptId,
        String conceptName,
        AdjustmentType type,
        BigDecimal percentage,
        String reason,
        boolean active) {
}
