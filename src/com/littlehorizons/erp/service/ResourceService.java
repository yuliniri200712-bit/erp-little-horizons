package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.enums.BorrowerType;
import com.littlehorizons.erp.model.enums.LoanStatus;
import com.littlehorizons.erp.model.enums.ResourceStatus;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.enums.TeacherStatus;
import com.littlehorizons.erp.util.TransactionManager;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Inventory and loans to students or teachers. A resource can only be lent when it is
 * 'Disponible'; returning it records the real date and makes it available again.
 */
public class ResourceService {

    public static final int MAX_OPEN_LOANS_PER_STUDENT = 3;
    private static final int MAX_LOAN_DAYS = 60;

    private final ResourceDao resourceDao;
    private final LoanDao loanDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public ResourceService(ResourceDao resourceDao, LoanDao loanDao, StudentDao studentDao, TeacherDao teacherDao) {
        this.resourceDao = resourceDao;
        this.loanDao = loanDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
    }

    // ================================================================ inventory

    public List<ResourceCategory> categories() {
        return resourceDao.findCategories();
    }

    public List<Resource> listResources(Integer categoryId, ResourceStatus status) {
        return resourceDao.findByFilter(categoryId, status);
    }

    public List<Resource> searchResources(String text) {
        return resourceDao.search(Validator.requireText(text, "Texto de búsqueda", 100));
    }

    public Resource getResource(int id) {
        return resourceDao.findById(id).orElseThrow(() -> new BusinessException("No existe el recurso con ID " + id + "."));
    }

    public Resource findResource(String idOrCode) {
        String key = Validator.requireText(idOrCode, "ID o código del recurso", 50).toUpperCase();
        Optional<Resource> byCode = resourceDao.findByCode(key);
        if (byCode.isPresent()) {
            return byCode.get();
        }
        if (key.matches("\\d{1,9}")) {
            return getResource(Integer.parseInt(key));
        }
        throw new BusinessException("No se encontró el recurso '" + key + "'.");
    }

    public int createResource(Resource resource) {
        if (resource.status() == ResourceStatus.LENT) {
            throw new BusinessException("Un recurso nuevo no puede crearse como 'Prestado'.");
        }
        return resourceDao.insert(validate(resource, null));
    }

    public void updateResource(Resource resource) {
        getResource(resource.id());
        resourceDao.update(validate(resource, resource.id()));
    }

    /** Manual status change (maintenance, decommission...). 'Prestado' is only set through a loan. */
    public void changeStatus(int resourceId, ResourceStatus newStatus) {
        Resource resource = getResource(resourceId);
        if (newStatus == ResourceStatus.LENT) {
            throw new BusinessException("El estado 'Prestado' se asigna automáticamente al registrar un préstamo.");
        }
        if (resource.status() == ResourceStatus.LENT) {
            throw new BusinessException("El recurso está prestado; registre la devolución antes de cambiar su estado.");
        }
        if (resource.status() == newStatus) {
            throw new BusinessException("El recurso ya está en estado '" + newStatus + "'.");
        }
        resourceDao.updateStatus(resourceId, newStatus);
    }

    public void deleteResource(int resourceId) {
        Resource resource = getResource(resourceId);
        boolean hasHistory = Arrays.stream(BorrowerType.values())
                .anyMatch(type -> !loanDao.findByResource(type, resourceId).isEmpty());
        if (resource.status() == ResourceStatus.LENT || hasHistory) {
            throw new BusinessException("El recurso tiene préstamos registrados; márquelo como 'Dado de Baja' para conservar el historial.");
        }
        resourceDao.delete(resourceId);
    }

    private Resource validate(Resource r, Integer currentId) {
        String code = Validator.requireText(r.code(), "Código", 50).toUpperCase();
        if (!code.matches("^[A-Z0-9-]{3,50}$")) {
            throw new BusinessException("El código solo admite letras, números y guiones (ejemplo: BIB-015).");
        }
        resourceDao.findByCode(code).filter(o -> !o.id().equals(currentId)).ifPresent(o -> {
            throw new BusinessException("Ya existe un recurso con el código " + code + ".");
        });
        String categoryName = categories().stream().filter(c -> c.id() == r.categoryId()).findFirst()
                .orElseThrow(() -> new BusinessException("La categoría " + r.categoryId() + " no existe.")).name();
        Validator.requireNotFuture(r.acquisitionDate(), "Fecha de adquisición");
        return new Resource(r.id(), code, Validator.requireText(r.name(), "Nombre", 150), r.categoryId(), categoryName,
                Validator.optionalText(r.description(), "Descripción", 1000), r.acquisitionDate(),
                Validator.requireText(r.location(), "Ubicación", 100), r.status() == null ? ResourceStatus.AVAILABLE : r.status());
    }

    // ================================================================ loans

    /**
     * Lends a resource. The resource row is locked (SELECT ... FOR UPDATE) so it cannot be lent
     * twice; it must be 'Disponible' and the borrower must be active.
     */
    public int lend(BorrowerType type, int borrowerId, int resourceId, LocalDate loanDate, LocalDate expectedReturn, String notes) {
        LocalDate start = loanDate == null ? LocalDate.now() : loanDate;
        Validator.requireNotFuture(start, "Fecha de préstamo");
        if (expectedReturn == null) {
            throw new BusinessException("La fecha de devolución prevista es obligatoria.");
        }
        Validator.requireOrder(start, expectedReturn, "Fecha de préstamo", "Devolución prevista");
        if (ChronoUnit.DAYS.between(start, expectedReturn) > MAX_LOAN_DAYS) {
            throw new BusinessException("Un préstamo no puede superar " + MAX_LOAN_DAYS + " días.");
        }
        requireActiveBorrower(type, borrowerId);
        return TransactionManager.inTransaction(() -> {
            Resource resource = resourceDao.findByIdForUpdate(resourceId)
                    .orElseThrow(() -> new BusinessException("No existe el recurso con ID " + resourceId + "."));
            if (resource.status() != ResourceStatus.AVAILABLE) {
                throw new BusinessException("El recurso " + resource.code() + " no está disponible (estado: " + resource.status() + ").");
            }
            int id = loanDao.insert(new Loan(null, type, resourceId, null, null, borrowerId, null, start, expectedReturn, null,
                    LoanStatus.ACTIVE, Validator.optionalText(notes, "Observaciones", 500)));
            resourceDao.updateStatus(resourceId, ResourceStatus.LENT);
            return id;
        });
    }

    private void requireActiveBorrower(BorrowerType type, int borrowerId) {
        if (type == BorrowerType.STUDENT) {
            var student = studentDao.findById(borrowerId)
                    .orElseThrow(() -> new BusinessException("No existe un estudiante con ID " + borrowerId + "."));
            if (student.status() != StudentStatus.ACTIVE) {
                throw new BusinessException("Solo se presta a estudiantes activos.");
            }
            long open = loanDao.findByBorrower(type, borrowerId).stream().filter(l -> l.status() != LoanStatus.RETURNED).count();
            if (open >= MAX_OPEN_LOANS_PER_STUDENT) {
                throw new BusinessException("El estudiante ya tiene " + open + " préstamos abiertos (máximo " + MAX_OPEN_LOANS_PER_STUDENT + ").");
            }
            if (loanDao.findByBorrower(type, borrowerId).stream().anyMatch(l -> l.status() == LoanStatus.OVERDUE)) {
                throw new BusinessException("El estudiante tiene préstamos vencidos; debe devolverlos antes de un nuevo préstamo.");
            }
        } else {
            var teacher = teacherDao.findById(borrowerId)
                    .orElseThrow(() -> new BusinessException("No existe un profesor con ID " + borrowerId + "."));
            if (teacher.status() != TeacherStatus.ACTIVE) {
                throw new BusinessException("Solo se presta a profesores activos.");
            }
        }
    }

    /** Registers the return: real date, loan 'Devuelto' and resource back to 'Disponible'. Returns days late. */
    public long returnLoan(BorrowerType type, int loanId, LocalDate returnDate, String notes) {
        Loan loan = loanDao.findById(type, loanId)
                .orElseThrow(() -> new BusinessException("No existe el préstamo " + loanId + " de " + type + "."));
        if (loan.status() == LoanStatus.RETURNED) {
            throw new BusinessException("El préstamo ya fue devuelto el " + loan.actualReturnDate() + ".");
        }
        LocalDate date = returnDate == null ? LocalDate.now() : returnDate;
        Validator.requireNotFuture(date, "Fecha de devolución");
        Validator.requireOrder(loan.loanDate(), date, "Fecha de préstamo", "Fecha de devolución");
        TransactionManager.inTransaction(() -> {
            loanDao.markReturned(type, loanId, date, Validator.optionalText(notes, "Observaciones", 500));
            resourceDao.updateStatus(loan.resourceId(), ResourceStatus.AVAILABLE);
        });
        return Math.max(0, ChronoUnit.DAYS.between(loan.expectedReturnDate(), date));
    }

    /** Marks active loans past their expected date as 'Vencido' in both tables. */
    public int refreshOverdue() {
        return Arrays.stream(BorrowerType.values()).mapToInt(loanDao::refreshOverdue).sum();
    }

    public List<Loan> openLoans() {
        refreshOverdue();
        return Arrays.stream(BorrowerType.values())
                .flatMap(type -> loanDao.findOpen(type).stream())
                .sorted(Comparator.comparing(Loan::expectedReturnDate))
                .toList();
    }

    public List<Loan> overdueLoans() {
        refreshOverdue();
        return Arrays.stream(BorrowerType.values())
                .flatMap(type -> loanDao.findOverdue(type).stream())
                .sorted(Comparator.comparingLong(Loan::daysOverdue).reversed())
                .toList();
    }

    public List<Loan> loansOf(BorrowerType type, int borrowerId) {
        refreshOverdue();
        return loanDao.findByBorrower(type, borrowerId);
    }

    public List<Loan> historyOfResource(int resourceId) {
        getResource(resourceId);
        return Arrays.stream(BorrowerType.values())
                .flatMap(type -> loanDao.findByResource(type, resourceId).stream())
                .sorted(Comparator.comparing(Loan::loanDate).reversed())
                .toList();
    }
}
