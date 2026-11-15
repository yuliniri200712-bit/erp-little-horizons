package com.littlehorizons.erp.model.report;

import com.littlehorizons.erp.model.enums.EventType;
import java.time.LocalDate;

/** Participation summary of an event. */
public record EventSummary(
        int eventId,
        String eventName,
        EventType type,
        LocalDate date,
        boolean hasCompetition,
        long participants,
        String podium) {
}
