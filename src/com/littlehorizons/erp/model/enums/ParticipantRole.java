package com.littlehorizons.erp.model.enums;

/** Role of a student in an event. */
public enum ParticipantRole implements DbEnum {
    PARTICIPANT("Participante"),
    COMPETITOR("Competidor"),
    ORGANIZER("Organizador"),
    ATTENDEE("Asistente");

    private final String label;

    ParticipantRole(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}