package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.enums.Placement;

import java.util.List;
import java.util.Optional;

/** Event participants (participantes_evento). */
public interface EventParticipantDao {

    int insert(EventParticipant p);

    Optional<EventParticipant> findById(int id);

    List<EventParticipant> findByEvent(int eventId);

    List<EventParticipant> findByStudent(int studentId);

    boolean exists(int eventId, int studentId);

    boolean isPlacementTaken(int eventId, Placement placement, int excludeParticipationId);

    boolean updatePlacement(int participationId, Placement placement);

    boolean delete(int id);
}
