package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.EventType;
import java.time.LocalDate;

/** School event. Table: eventos. */
public record SchoolEvent(
        Integer id,
        String name,
        String description,
        EventType type,
        LocalDate date,
        String place,
        boolean hasCompetition,
        Integer organizerId,
        String organizerName,
        boolean active) {
}
