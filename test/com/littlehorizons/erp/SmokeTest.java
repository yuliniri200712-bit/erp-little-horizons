package com.littlehorizons.erp;

import com.littlehorizons.erp.config.AppConfig;
import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.Charge;
import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.GuardianLink;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.enums.AcademicPeriod;
import com.littlehorizons.erp.model.enums.BorrowerType;
import com.littlehorizons.erp.model.enums.ChargeStatus;
import com.littlehorizons.erp.model.enums.ParticipantRole;
import com.littlehorizons.erp.model.enums.PaymentMethod;
import com.littlehorizons.erp.model.enums.PaymentPeriod;
import com.littlehorizons.erp.model.enums.Placement;
import com.littlehorizons.erp.model.enums.ResourceStatus;
import com.littlehorizons.erp.service.PaymentRequest;
import com.littlehorizons.erp.service.PaymentResult;
import com.littlehorizons.erp.util.TransactionManager;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Integration smoke tests of the main business rules against the MySQL database loaded with
 * docs/database.sql. Everything runs inside ONE transaction that is rolled back at the end,
 * so the database is left unchanged (only the generated test PDF remains in facturas/).
 *
 * <p>Run: {@code mvn -q test-compile} and then
 * {@code java -cp "target/erp-little-horizons-1.0.0.jar;target/test-classes" com.littlehorizons.erp.SmokeTest}</p>
 */
public final class SmokeTest {

    private static final List<String> FAILURES = new ArrayList<>();
    private static int passed;

    private static final class Rollback extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private SmokeTest() {
    }

    public static void main(String[] args) {
        AppServices s = AppServices.create(AppConfig.getInstance());
        try {
            TransactionManager.inTransaction(() -> {
                gradeRules(s);
                clubRules(s);
                loanRules(s);
                paymentAndInvoice(s);
                scheduleConflict(s);
                studentRules(s);
                eventRules(s);
                attendanceReport(s);
                throw new Rollback();
            });
        } catch (Rollback expected) {
            System.out.println("(cambios revertidos: rollback de la transacción de prueba)");
        }
        System.out.println();
        System.out.println("RESULTADO: " + passed + " pruebas OK, " + FAILURES.size() + " fallidas");
        FAILURES.forEach(f -> System.out.println("  FALLA: " + f));
        System.exit(FAILURES.isEmpty() ? 0 : 1);
    }

    // ---------------------------------------------------------------- tests

    private static void gradeRules(AppServices s) {
        expectError("nota 5.5 fuera de rango", () -> s.academic().parseScore("5.5"));
        expectError("nota sin decimal (4)", () -> s.academic().parseScore("4"));
        expectError("nota con dos decimales (4.25)", () -> s.academic().parseScore("4.25"));
        expectError("nota negativa (-1.0)", () -> s.academic().parseScore("-1.0"));
        check("nota 3,5 con coma aceptada", s.academic().parseScore("3,5").compareTo(new BigDecimal("3.5")) == 0);
        check("nota 0.0 aceptada", s.academic().parseScore("0.0").signum() == 0);
        expectError("nota a materia no inscrita", () ->
                s.academic().saveGrade(1, 5, 1, 2026, AcademicPeriod.IV, new BigDecimal("4.0"), null, LocalDate.now()));
        expectError("profesor no asignado a la materia", () ->
                s.academic().saveGrade(1, 1, 2, 2026, AcademicPeriod.IV, new BigDecimal("4.0"), null, LocalDate.now()));
        AcademicRecord saved = s.academic().saveGrade(1, 1, 1, 2026, AcademicPeriod.IV, new BigDecimal("4.8"), "prueba", LocalDate.now());
        check("nota válida actualizada", s.academic().findGrade(1, 1, 2026, AcademicPeriod.IV)
                .map(r -> r.score().compareTo(new BigDecimal("4.8")) == 0).orElse(false) && saved.id() != null);
        try {
            new AcademicRecordDao().insert(new AcademicRecord(null, 2, 1, null, 1, null, 2026, AcademicPeriod.FINAL,
                    new BigDecimal("6.0"), null, LocalDate.now()));
            fail("CHECK de BD debía rechazar nota 6.0");
        } catch (DataAccessException e) {
            pass("CHECK de BD rechaza nota 6.0 -> " + e.getUserMessage());
        }
        check("promedios por materia calculados", !s.academic().subjectAverages(1, 2026).isEmpty());
        check("promedios por período (4 períodos)", s.academic().periodAverages(1, 2026).size() == 4);
        check("promedio del curso por materia (Preescolar, 4 materias)", s.academic().courseAverages(1, 2026, null).size() == 4);
        check("historial del graduado incluye 2025", s.academic().academicHistory(37).stream().anyMatch(y -> y.year() == 2025));
    }

    private static void clubRules(AppServices s) {
        expectError("club con cupo completo (Ajedrez 3/3)", () -> s.extracurricular().joinClub(5, 1, LocalDate.now()));
        expectError("membresía duplicada", () -> s.extracurricular().joinClub(20, 1, LocalDate.now()));
        int before = s.extracurricular().getClub(2).activeMembers();
        int id = s.extracurricular().joinClub(5, 2, LocalDate.now());
        check("inscripción con cupo disponible", s.extracurricular().getClub(2).activeMembers() == before + 1);
        s.extracurricular().leaveClub(id, LocalDate.now(), "prueba");
        check("retiro de club libera cupo", s.extracurricular().getClub(2).activeMembers() == before);
    }

    private static void loanRules(AppServices s) {
        expectError("préstamo de recurso en mantenimiento", () ->
                s.resources().lend(BorrowerType.STUDENT, 1, 17, LocalDate.now(), LocalDate.now().plusDays(7), null));
        int loanId = s.resources().lend(BorrowerType.STUDENT, 1, 1, LocalDate.now(), LocalDate.now().plusDays(7), null);
        check("préstamo crea registro y marca Prestado", s.resources().getResource(1).status() == ResourceStatus.LENT);
        expectError("doble préstamo del mismo recurso", () ->
                s.resources().lend(BorrowerType.TEACHER, 1, 1, LocalDate.now(), LocalDate.now().plusDays(7), null));
        long late = s.resources().returnLoan(BorrowerType.STUDENT, loanId, LocalDate.now(), "buen estado");
        check("devolución deja recurso Disponible", s.resources().getResource(1).status() == ResourceStatus.AVAILABLE && late == 0);
        expectError("devolver préstamo ya devuelto", () -> s.resources().returnLoan(BorrowerType.STUDENT, loanId, LocalDate.now(), null));
        expectError("estudiante con préstamo vencido no puede pedir otro", () ->
                s.resources().lend(BorrowerType.STUDENT, 14, 2, LocalDate.now(), LocalDate.now().plusDays(7), null));
        List<Loan> overdue = s.resources().overdueLoans();
        check("reporte de vencidos (5 préstamos)", overdue.size() == 5);
        int teacherLoan = s.resources().lend(BorrowerType.TEACHER, 1, 2, LocalDate.now(), LocalDate.now().plusDays(10), null);
        check("préstamo a profesor en tabla prestamos_profesor",
                s.resources().loansOf(BorrowerType.TEACHER, 1).stream().anyMatch(l -> l.id() == teacherLoan));
    }

    private static void paymentAndInvoice(AppServices s) throws RuntimeException {
        int debtsBefore = s.finance().debtsOverOneMonth().size();
        Charge september = s.finance().chargesOf(8, true).stream()
                .filter(c -> c.description().startsWith("Pensión septiembre")).findFirst().orElseThrow();
        expectError("pago mayor al saldo del cobro", () -> s.finance().registerPayment(new PaymentRequest(8, null,
                september.conceptId(), september.id(), september.pending().add(BigDecimal.ONE), LocalDate.now(),
                PaymentMethod.CASH, PaymentPeriod.III, null)));
        expectError("pago con fecha futura", () -> s.finance().registerPayment(new PaymentRequest(8, null,
                september.conceptId(), september.id(), september.pending(), LocalDate.now().plusDays(2),
                PaymentMethod.CASH, PaymentPeriod.III, null)));
        expectError("acudiente no vinculado al estudiante", () -> s.finance().registerPayment(new PaymentRequest(8, 1,
                september.conceptId(), september.id(), september.pending(), LocalDate.now(), PaymentMethod.CASH, null, null)));
        PaymentResult result = s.finance().registerPayment(new PaymentRequest(8, null, september.conceptId(), september.id(),
                september.pending(), LocalDate.now(), PaymentMethod.TRANSFER, PaymentPeriod.III, "Prueba automatizada"));
        Invoice invoice = result.invoice();
        check("recibo consecutivo generado (" + result.payment().receiptNumber() + ")", result.payment().receiptNumber().startsWith("RC-"));
        check("factura consecutiva " + invoice.number(), invoice.number().matches("FE-\\d{6}"));
        check("CUFE simulado SHA-384 (96 hex)", invoice.cufe().matches("[0-9a-f]{96}"));
        check("subtotal + IVA = total", invoice.subtotal().add(invoice.tax()).compareTo(invoice.total()) == 0);
        check("PDF generado " + result.pdfPath(), result.pdfPath() != null && Files.exists(result.pdfPath())
                && result.pdfPath().getFileName().toString().matches("factura_\\d{6}\\.pdf"));
        check("cobro pasa a Pagado", s.finance().getCharge(september.id()).status() == ChargeStatus.PAID);
        check("deuda sale del reporte de mora", s.finance().debtsOverOneMonth().size() == debtsBefore - 1);
        check("historial por acudiente incluye el pago", s.finance().paymentsOfGuardian(result.payment().guardianId()).stream()
                .anyMatch(p -> p.id().equals(result.payment().id())));
        BigDecimal uniform = s.finance().effectiveAmount(1, 3);
        check("IVA 19% en uniforme (250.000 -> 297.500)", uniform.compareTo(new BigDecimal("297500")) == 0);
        check("beca 50% en pensión (350.000 -> 175.000)", s.finance().effectiveAmount(4, 2).compareTo(new BigDecimal("175000")) == 0);
    }

    private static void scheduleConflict(AppServices s) {
        ClassSchedule block = s.teachers().scheduleOfAssignment(1).getFirst();
        expectError("conflicto de horario del grupo", () ->
                s.teachers().addSchedule(2, block.day(), block.startTime(), block.endTime(), null));
        expectError("hora fin antes de inicio", () ->
                s.teachers().addSchedule(2, block.day(), block.endTime(), block.startTime(), null));
        expectError("bloque fuera de la jornada mañana", () ->
                s.teachers().addSchedule(2, block.day(), java.time.LocalTime.of(15, 0), java.time.LocalTime.of(16, 0), null));
        expectError("asignar materia ya asignada en la misma sección", () ->
                s.teachers().assign(2, 1, 2026, "A", null));
    }

    private static void studentRules(AppServices s) {
        List<GuardianLink> links = s.students().guardiansOf(3);
        check("estudiante con al menos un acudiente", !links.isEmpty());
        if (links.size() == 1) {
            expectError("desvincular el único acudiente", () -> s.students().unlinkGuardian(3, links.getFirst().guardian().id()));
        }
        expectError("matrícula duplicada en el año", () -> s.students().enroll(1, 1, 2026, LocalDate.now(), null));
        expectError("inscripción a materia de otro grado", () -> s.academic().enrollInSubject(1, 5, 2026, AcademicPeriod.I, "A"));
        expectError("eliminar estudiante con pagos", () -> s.students().deleteStudent(1));
        expectError("graduar a estudiante que no está en Once", () ->
                s.students().changeStatus(1, com.littlehorizons.erp.model.enums.StudentStatus.GRADUATED, LocalDate.now(), null));
        check("hermanos comparten acudiente (acudiente 1 con 2 hijos)", s.students().studentsOf(1).size() >= 2);
    }

    private static void eventRules(AppServices s) {
        int outsider = IntStream.rangeClosed(1, 36)
                .filter(id -> s.extracurricular().participantsOf(1).stream().noneMatch(p -> p.studentId() == id))
                .findFirst().orElseThrow();
        expectError("1er lugar repetido en un evento", () ->
                s.extracurricular().addParticipant(1, outsider, ParticipantRole.COMPETITOR, Placement.FIRST, null));
        expectError("puesto en evento sin competencia", () ->
                s.extracurricular().addParticipant(4, outsider, ParticipantRole.PARTICIPANT, Placement.FIRST, null));
        int id = s.extracurricular().addParticipant(1, outsider, ParticipantRole.COMPETITOR, Placement.PARTICIPANT, null);
        check("participante registrado en evento", id > 0);
    }

    private static void attendanceReport(AppServices s) {
        LocalDate from = LocalDate.of(2026, 11, 2);
        LocalDate to = LocalDate.of(2026, 11, 15);
        check("alerta >3 ausencias/semana incluye estudiante 6",
                s.attendance().weeklyAbsenceAlerts(from, to).stream().anyMatch(a -> a.studentId() == 6));
        check("estudiante con 3 días ausente NO genera alerta (29)",
                s.attendance().weeklyAbsenceAlerts(from, to).stream().noneMatch(a -> a.studentId() == 29));
        expectError("asistencia con fecha futura", () -> s.attendance().record(1, 1, LocalDate.now().plusDays(1),
                com.littlehorizons.erp.model.enums.AttendanceStatus.PRESENT, null, null));
    }

    // ---------------------------------------------------------------- helpers

    private static void expectError(String name, Runnable action) {
        try {
            action.run();
            fail(name + " (se esperaba un error de validación)");
        } catch (BusinessException e) {
            pass(name + " -> " + e.getMessage().lines().findFirst().orElse(""));
        }
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            pass(name);
        } else {
            fail(name);
        }
    }

    private static void pass(String name) {
        passed++;
        System.out.println("[OK]    " + name);
    }

    private static void fail(String name) {
        FAILURES.add(name);
        System.out.println("[FALLA] " + name);
    }
}
