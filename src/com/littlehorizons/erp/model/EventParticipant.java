package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.ParticipantRole;
import com.littlehorizons.erp.model.enums.Placement;

/** Participation of a student in an event. Table: participantes_evento. */
public record EventParticipant(
        Integer id,
        int eventId,
        String eventName,
        int studentId,
        String studentName,
        ParticipantRole role,
        Placement placement,
        String notes) {
}
