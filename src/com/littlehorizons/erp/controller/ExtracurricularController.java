package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.enums.ParticipantRole;
import com.littlehorizons.erp.model.enums.Placement;
import com.littlehorizons.erp.service.ExtracurricularService;
import com.littlehorizons.erp.service.ReportService;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.ExtracurricularView;

import java.time.LocalDate;
import java.util.List;

/** Menu 4 - Actividades Extracurriculares: clubs and events. */
public class ExtracurricularController extends BaseController {

    private final ExtracurricularService service;
    private final ReportService reportService;
    private final ExtracurricularView extraView;
    private final EntityPicker picker;

    public ExtracurricularController(ConsoleView view, ExtracurricularService service, ReportService reportService,
                                     EntityPicker picker) {
        super(view);
        this.service = service;
        this.reportService = reportService;
        this.extraView = new ExtracurricularView(view);
        this.picker = picker;
    }

    @Override
    public void start() {
        runMenu("ACTIVIDADES EXTRACURRICULARES", List.of(
                Option.submenu("Clubes", this::clubsMenu),
                Option.submenu("Eventos", this::eventsMenu),
                Option.action("Actividades de un estudiante (clubes y eventos)", this::activitiesOfStudent)));
    }

    // ---------------------------------------------------------------- clubs

    private void clubsMenu() {
        runMenu("CLUBES", List.of(
                Option.action("Catálogo de clubes", () -> extraView.showClubs(service.listClubs())),
                Option.action("Registrar club", () -> {
                    Integer supervisor = picker.optionalTeacherId("Profesor encargado");
                    int id = service.createClub(extraView.readClub(null, supervisor));
                    view.success("Club registrado con ID " + id + ".");
                }),
                Option.action("Actualizar club", () -> {
                    Club club = picker.club();
                    Integer supervisor = club.supervisorId();
                    if (view.confirm("¿Cambiar profesor encargado (actual: " + club.supervisorName() + ")?")) {
                        supervisor = picker.optionalTeacherId("Nuevo profesor encargado");
                    }
                    service.updateClub(extraView.readClub(club, supervisor));
                    view.success("Club actualizado.");
                }),
                Option.action("Eliminar club", () -> {
                    Club club = picker.club();
                    if (view.confirm("¿Confirma eliminar el club " + club.name() + "?")) {
                        service.deleteClub(club.id());
                        view.success("Club eliminado.");
                    }
                }),
                Option.action("Inscribir estudiante en un club", () -> {
                    Student student = picker.student();
                    Club club = picker.club();
                    LocalDate date = view.readDate("Fecha de ingreso", LocalDate.now());
                    int id = service.joinClub(student.id(), club.id(), date);
                    Club updated = service.getClub(club.id());
                    view.success("Membresía #" + id + " creada. Cupo " + updated.activeMembers() + "/" + updated.maxCapacity() + ".");
                }),
                Option.action("Retirar estudiante de un club", () -> {
                    Club club = picker.club();
                    extraView.showMemberships(service.membersOf(club.id(), true), false);
                    int id = view.readInt("ID de la membresía", 1, Integer.MAX_VALUE);
                    LocalDate date = view.readDate("Fecha de retiro", LocalDate.now());
                    service.leaveClub(id, date, view.readOptional("Motivo del retiro", null));
                    view.success("Estudiante retirado del club.");
                }),
                Option.action("Ver miembros de un club", () -> {
                    Club club = picker.club();
                    boolean activeOnly = view.confirm("¿Mostrar solo miembros activos?");
                    extraView.showMemberships(service.membersOf(club.id(), activeOnly), false);
                })));
    }

    // ---------------------------------------------------------------- events

    private void eventsMenu() {
        runMenu("EVENTOS", List.of(
                Option.action("Listar eventos", () -> extraView.showEvents(service.listEvents())),
                Option.action("Registrar evento", () -> {
                    Integer organizer = picker.optionalTeacherId("Profesor organizador");
                    int id = service.createEvent(extraView.readEvent(null, organizer));
                    view.success("Evento registrado con ID " + id + ".");
                }),
                Option.action("Actualizar evento", () -> {
                    SchoolEvent event = picker.event();
                    service.updateEvent(extraView.readEvent(event, event.organizerId()));
                    view.success("Evento actualizado.");
                }),
                Option.action("Eliminar evento", () -> {
                    SchoolEvent event = picker.event();
                    if (view.confirm("Se eliminarán también sus participantes. ¿Confirma eliminar " + event.name() + "?")) {
                        service.deleteEvent(event.id());
                        view.success("Evento eliminado.");
                    }
                }),
                Option.action("Registrar participante", this::addParticipant),
                Option.action("Asignar / cambiar puesto de un participante", () -> {
                    SchoolEvent event = picker.event();
                    extraView.showParticipants(service.participantsOf(event.id()), false);
                    int id = view.readInt("ID de la participación", 1, Integer.MAX_VALUE);
                    Placement placement = view.chooseOptionalEnum("Puesto (Enter = sin puesto)", Placement.values());
                    service.updatePlacement(id, placement);
                    view.success("Puesto actualizado.");
                }),
                Option.action("Quitar participante", () -> {
                    SchoolEvent event = picker.event();
                    extraView.showParticipants(service.participantsOf(event.id()), false);
                    service.removeParticipant(view.readInt("ID de la participación", 1, Integer.MAX_VALUE));
                    view.success("Participante retirado del evento.");
                }),
                Option.action("Ver participantes de un evento", () -> {
                    SchoolEvent event = picker.event();
                    view.info(event.name() + " - " + event.type() + (event.hasCompetition() ? " (con competencia)" : ""));
                    extraView.showParticipants(service.participantsOf(event.id()), false);
                })));
    }

    private void addParticipant() {
        SchoolEvent event = picker.event();
        Student student = picker.student();
        ParticipantRole role = view.chooseEnum("Rol", ParticipantRole.values(),
                event.hasCompetition() ? ParticipantRole.COMPETITOR : ParticipantRole.PARTICIPANT);
        Placement placement = event.hasCompetition()
                ? view.chooseOptionalEnum("Puesto obtenido (Enter = aún sin puesto)", Placement.values()) : null;
        String notes = view.readOptional("Observaciones", null);
        int id = service.addParticipant(event.id(), student.id(), role, placement, notes);
        view.success("Participación #" + id + " registrada.");
    }

    private void activitiesOfStudent() {
        Student student = picker.student();
        view.section("Clubes");
        extraView.showMemberships(service.clubsOfStudent(student.id()), true);
        view.section("Eventos");
        List<EventParticipant> events = service.eventsOfStudent(student.id());
        extraView.showParticipants(events, true);
    }

    /** Reused by the reports menu. */
    void occupancyReport() {
        extraView.showOccupancy(service.clubsByOccupancy());
    }

    void eventsReport() {
        extraView.showEventSummaries(service.eventSummaries(readYear()));
    }

    void participationReport() {
        extraView.showParticipation(reportService.participationByStudent(readYear()));
    }
}
