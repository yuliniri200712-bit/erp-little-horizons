package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.ClubMembershipDao;
import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.EventParticipantDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.MembershipStatus;
import com.littlehorizons.erp.model.enums.ParticipantRole;
import com.littlehorizons.erp.model.enums.Placement;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.enums.TeacherStatus;
import com.littlehorizons.erp.model.report.EventSummary;
import com.littlehorizons.erp.util.TransactionManager;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Clubs (catalog, capacity-checked memberships) and events (participants and podium positions).
 */
public class ExtracurricularService {

    private static final int MAX_CLUB_CAPACITY = 200;

    private final ClubDao clubDao;
    private final ClubMembershipDao membershipDao;
    private final EventDao eventDao;
    private final EventParticipantDao participantDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public ExtracurricularService(ClubDao clubDao, ClubMembershipDao membershipDao, EventDao eventDao,
                                  EventParticipantDao participantDao, StudentDao studentDao, TeacherDao teacherDao) {
        this.clubDao = clubDao;
        this.membershipDao = membershipDao;
        this.eventDao = eventDao;
        this.participantDao = participantDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
    }

    // ================================================================ clubs

    public List<Club> listClubs() {
        return clubDao.findAll();
    }

    /** Clubs sorted by occupancy (active members / capacity), fullest first. */
    public List<Club> clubsByOccupancy() {
        return clubDao.findAll().stream()
                .sorted(Comparator.comparingDouble((Club c) -> (double) c.activeMembers() / c.maxCapacity()).reversed())
                .toList();
    }

    public Club getClub(int id) {
        return clubDao.findById(id).orElseThrow(() -> new BusinessException("No existe un club con ID " + id + "."));
    }

    public int createClub(Club club) {
        return clubDao.insert(validateClub(club, 0));
    }

    public void updateClub(Club club) {
        Club current = getClub(club.id());
        clubDao.update(validateClub(club, current.activeMembers()));
    }

    public void deleteClub(int clubId) {
        Club club = getClub(clubId);
        if (club.activeMembers() > 0) {
            throw new BusinessException("El club tiene " + club.activeMembers() + " miembros activos. Retírelos o desactive el club.");
        }
        clubDao.delete(clubId);
    }

    private Club validateClub(Club c, int activeMembers) {
        Validator.requireRange(c.maxCapacity(), 1, MAX_CLUB_CAPACITY, "Cupo máximo");
        if (c.maxCapacity() < activeMembers) {
            throw new BusinessException("El cupo no puede ser menor que los miembros activos (" + activeMembers + ").");
        }
        if (c.supervisorId() != null) {
            requireActiveTeacher(c.supervisorId());
        }
        return new Club(c.id(), Validator.requireText(c.name(), "Nombre del club", 100),
                Validator.optionalText(c.description(), "Descripción", 1000), c.supervisorId(), null, c.maxCapacity(),
                Validator.optionalText(c.meetingSchedule(), "Horario de reunión", 100), c.active(), activeMembers);
    }

    /**
     * Enrolls a student in a club. The club row is locked (SELECT ... FOR UPDATE) so two
     * simultaneous enrollments cannot exceed the capacity.
     */
    public int joinClub(int studentId, int clubId, LocalDate joinDate) {
        Student student = requireActiveStudent(studentId);
        LocalDate date = joinDate == null ? LocalDate.now() : joinDate;
        Validator.requireNotFuture(date, "Fecha de ingreso");
        return TransactionManager.inTransaction(() -> {
            clubDao.lockForUpdate(clubId);
            Club club = getClub(clubId);
            if (!club.active()) {
                throw new BusinessException("El club " + club.name() + " está inactivo.");
            }
            if (membershipDao.hasActive(studentId, clubId)) {
                throw new BusinessException(student.fullName() + " ya es miembro activo de " + club.name() + ".");
            }
            long active = membershipDao.countActive(clubId);
            if (active >= club.maxCapacity()) {
                throw new BusinessException("Cupo completo: " + club.name() + " tiene " + active + "/" + club.maxCapacity() + " miembros.");
            }
            return membershipDao.insert(studentId, clubId, date);
        });
    }

    public void leaveClub(int membershipId, LocalDate date, String reason) {
        ClubMembership membership = membershipDao.findById(membershipId)
                .orElseThrow(() -> new BusinessException("No existe la membresía " + membershipId + "."));
        if (membership.status() != MembershipStatus.ACTIVE) {
            throw new BusinessException("La membresía ya está retirada.");
        }
        LocalDate effective = date == null ? LocalDate.now() : date;
        Validator.requireNotFuture(effective, "Fecha de retiro");
        Validator.requireOrder(membership.joinDate(), effective, "Fecha de ingreso", "Fecha de retiro");
        membershipDao.withdraw(membershipId, effective, Validator.optionalText(reason, "Motivo", 500));
    }

    public List<ClubMembership> membersOf(int clubId, boolean activeOnly) {
        getClub(clubId);
        return membershipDao.findByClub(clubId, activeOnly);
    }

    public List<ClubMembership> clubsOfStudent(int studentId) {
        return membershipDao.findByStudent(studentId);
    }

    // ================================================================ events

    public List<SchoolEvent> listEvents() {
        return eventDao.findAll();
    }

    public SchoolEvent getEvent(int id) {
        return eventDao.findById(id).orElseThrow(() -> new BusinessException("No existe un evento con ID " + id + "."));
    }

    public int createEvent(SchoolEvent event) {
        return eventDao.insert(validateEvent(event));
    }

    public void updateEvent(SchoolEvent event) {
        getEvent(event.id());
        if (!event.hasCompetition() && participantDao.findByEvent(event.id()).stream().anyMatch(p -> p.placement() != null)) {
            throw new BusinessException("El evento tiene puestos registrados; no puede marcarse sin competencia.");
        }
        eventDao.update(validateEvent(event));
    }

    public void deleteEvent(int eventId) {
        getEvent(eventId);
        eventDao.delete(eventId);
    }

    private SchoolEvent validateEvent(SchoolEvent e) {
        if (e.type() == null || e.date() == null) {
            throw new BusinessException("Tipo y fecha del evento son obligatorios.");
        }
        if (e.organizerId() != null) {
            teacherDao.findById(e.organizerId())
                    .orElseThrow(() -> new BusinessException("No existe el profesor organizador " + e.organizerId() + "."));
        }
        return new SchoolEvent(e.id(), Validator.requireText(e.name(), "Nombre del evento", 150),
                Validator.optionalText(e.description(), "Descripción", 1000), e.type(), e.date(),
                Validator.optionalText(e.place(), "Lugar", 150), e.hasCompetition(), e.organizerId(), null, e.active());
    }

    /** Registers a participant; positions are only valid in competitions and 1st-3rd are unique. */
    public int addParticipant(int eventId, int studentId, ParticipantRole role, Placement placement, String notes) {
        SchoolEvent event = getEvent(eventId);
        if (!event.active()) {
            throw new BusinessException("El evento está inactivo.");
        }
        Student student = requireActiveStudent(studentId);
        if (participantDao.exists(eventId, studentId)) {
            throw new BusinessException(student.fullName() + " ya está registrado en " + event.name() + ".");
        }
        validatePlacement(event, placement, 0);
        return participantDao.insert(new EventParticipant(null, eventId, null, studentId, null,
                role == null ? ParticipantRole.PARTICIPANT : role, placement, Validator.optionalText(notes, "Observaciones", 500)));
    }

    public void updatePlacement(int participationId, Placement placement) {
        EventParticipant participant = participantDao.findById(participationId)
                .orElseThrow(() -> new BusinessException("No existe la participación " + participationId + "."));
        validatePlacement(getEvent(participant.eventId()), placement, participationId);
        participantDao.updatePlacement(participationId, placement);
    }

    private void validatePlacement(SchoolEvent event, Placement placement, int participationId) {
        if (placement == null) {
            return;
        }
        if (!event.hasCompetition()) {
            throw new BusinessException("El evento " + event.name() + " no tiene competencia; no se asignan puestos.");
        }
        if (placement.isPodium() && participantDao.isPlacementTaken(event.id(), placement, participationId)) {
            throw new BusinessException("El " + placement + " ya fue asignado en este evento.");
        }
    }

    public void removeParticipant(int participationId) {
        if (!participantDao.delete(participationId)) {
            throw new BusinessException("No existe la participación " + participationId + ".");
        }
    }

    public List<EventParticipant> participantsOf(int eventId) {
        getEvent(eventId);
        return participantDao.findByEvent(eventId);
    }

    public List<EventParticipant> eventsOfStudent(int studentId) {
        return participantDao.findByStudent(studentId);
    }

    public List<EventSummary> eventSummaries(int year) {
        return eventDao.summaries(year);
    }

    private Student requireActiveStudent(int studentId) {
        Student student = studentDao.findById(studentId)
                .orElseThrow(() -> new BusinessException("No existe un estudiante con ID " + studentId + "."));
        if (student.status() != StudentStatus.ACTIVE) {
            throw new BusinessException("El estudiante no está activo (estado: " + student.status() + ").");
        }
        return student;
    }

    private Teacher requireActiveTeacher(int teacherId) {
        Teacher teacher = teacherDao.findById(teacherId)
                .orElseThrow(() -> new BusinessException("No existe un profesor con ID " + teacherId + "."));
        if (teacher.status() != TeacherStatus.ACTIVE) {
            throw new BusinessException("El profesor encargado debe estar activo.");
        }
        return teacher;
    }
}
