package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.enums.EventType;
import com.littlehorizons.erp.model.report.EventSummary;
import com.littlehorizons.erp.model.report.StudentParticipation;
import com.littlehorizons.erp.util.Formatter;

import java.time.LocalDate;
import java.util.List;

import static com.littlehorizons.erp.view.ConsoleView.row;

/** Forms and listings of clubs, memberships, events and participants. */
public class ExtracurricularView {

    private final ConsoleView console;

    public ExtracurricularView(ConsoleView console) {
        this.console = console;
    }

    /** Reads the club form; the supervisor ID is selected by the controller. */
    public Club readClub(Club current, Integer supervisorId) {
        boolean isNew = current == null;
        console.section(isNew ? "Nuevo club" : "Actualizar club (Enter conserva el valor)");
        String name = console.readText("Nombre del club", isNew ? null : current.name());
        String description = console.readOptional("Descripción", isNew ? null : current.description());
        int capacity = console.readInt("Cupo máximo", 1, 200, isNew ? 20 : current.maxCapacity());
        String schedule = console.readOptional("Horario de reunión (ej: Martes 2:00-3:30 PM)", isNew ? null : current.meetingSchedule());
        boolean active = console.readBoolean("¿Activo?", isNew || current.active());
        return new Club(isNew ? null : current.id(), name, description, supervisorId, null, capacity, schedule, active,
                isNew ? 0 : current.activeMembers());
    }

    public SchoolEvent readEvent(SchoolEvent current, Integer organizerId) {
        boolean isNew = current == null;
        console.section(isNew ? "Nuevo evento" : "Actualizar evento (Enter conserva el valor)");
        String name = console.readText("Nombre del evento", isNew ? null : current.name());
        String description = console.readOptional("Descripción", isNew ? null : current.description());
        EventType type = console.chooseEnum("Tipo de evento", EventType.values(), isNew ? null : current.type());
        LocalDate date = console.readDate("Fecha del evento", isNew ? null : current.date());
        String place = console.readOptional("Lugar", isNew ? null : current.place());
        boolean competition = console.readBoolean("¿Tiene competencia (puestos)?", !isNew && current.hasCompetition());
        boolean active = console.readBoolean("¿Activo?", isNew || current.active());
        return new SchoolEvent(isNew ? null : current.id(), name, description, type, date, place, competition, organizerId, null, active);
    }

    public void showClubs(List<Club> clubs) {
        console.table(List.of("ID", "Club", "Encargado", "Cupo", "Activos", "Disponibles", "Horario", "Activo"),
                clubs.stream().map(c -> row(c.id(), c.name(), c.supervisorName(), c.maxCapacity(), c.activeMembers(),
                        Math.max(0, c.maxCapacity() - c.activeMembers()), c.meetingSchedule(), Formatter.yesNo(c.active()))).toList());
    }

    public void showOccupancy(List<Club> clubs) {
        console.table(List.of("Club", "Encargado", "Activos/Cupo", "Ocupación", "Estado"),
                clubs.stream().map(c -> {
                    int pct = Math.round(100f * c.activeMembers() / c.maxCapacity());
                    String state = !c.active() ? "Inactivo" : c.activeMembers() >= c.maxCapacity() ? "LLENO" : "Con cupo";
                    return row(c.name(), c.supervisorName(), c.activeMembers() + "/" + c.maxCapacity(), pct + "%", state);
                }).toList());
    }

    public void showMemberships(List<ClubMembership> memberships, boolean byStudent) {
        console.table(List.of("ID", byStudent ? "Club" : "Estudiante", "Ingreso", "Estado", "Retiro", "Motivo"),
                memberships.stream().map(m -> row(m.id(), byStudent ? m.clubName() : m.studentName(), Formatter.date(m.joinDate()),
                        m.status(), Formatter.date(m.withdrawalDate()), m.withdrawalReason())).toList());
    }

    public void showEvents(List<SchoolEvent> events) {
        console.table(List.of("ID", "Fecha", "Evento", "Tipo", "Lugar", "Competencia", "Organizador", "Activo"),
                events.stream().map(e -> row(e.id(), Formatter.date(e.date()), e.name(), e.type(), e.place(),
                        Formatter.yesNo(e.hasCompetition()), e.organizerName(), Formatter.yesNo(e.active()))).toList());
    }

    public void showParticipants(List<EventParticipant> participants, boolean byStudent) {
        console.table(List.of("ID", byStudent ? "Evento" : "Estudiante", "Rol", "Puesto", "Observaciones"),
                participants.stream().map(p -> row(p.id(), byStudent ? p.eventName() : p.studentName(), p.role(), p.placement(),
                        p.notes())).toList());
    }

    public void showEventSummaries(List<EventSummary> summaries) {
        console.table(List.of("Fecha", "Evento", "Tipo", "Competencia", "Participantes", "Podio"),
                summaries.stream().map(s -> row(Formatter.date(s.date()), s.eventName(), s.type(), Formatter.yesNo(s.hasCompetition()),
                        s.participants(), s.podium())).toList());
    }

    public void showParticipation(List<StudentParticipation> rows) {
        console.table(List.of("ID", "Estudiante", "Grado", "Clubes activos", "Eventos", "Podios"),
                rows.stream().map(r -> row(r.studentId(), r.studentName(), r.gradeName(), r.activeClubs(), r.events(), r.podiums())).toList());
    }
}
