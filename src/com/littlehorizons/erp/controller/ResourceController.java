package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.enums.BorrowerType;
import com.littlehorizons.erp.model.enums.ResourceStatus;
import com.littlehorizons.erp.service.ResourceService;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.ResourceView;

import java.time.LocalDate;
import java.util.List;

/** Menu 6 - Gestión de Recursos: inventory (library, labs, sports, devices) and loans. */
public class ResourceController extends BaseController {

    private final ResourceService service;
    private final ResourceView resourceView;
    private final EntityPicker picker;

    public ResourceController(ConsoleView view, ResourceService service, EntityPicker picker) {
        super(view);
        this.service = service;
        this.resourceView = new ResourceView(view);
        this.picker = picker;
    }

    @Override
    public void start() {
        runMenu("GESTIÓN DE RECURSOS (BIBLIOTECA Y LABORATORIO)", List.of(
                Option.action("Listar inventario (filtro por categoría y estado)", this::listInventory),
                Option.action("Buscar recurso por código o nombre", () ->
                        resourceView.showResources(service.searchResources(view.readRequired("Texto a buscar")))),
                Option.action("Registrar recurso", () -> {
                    int id = service.createResource(resourceView.readResource(null, service.categories()));
                    view.success("Recurso registrado con ID " + id + ".");
                }),
                Option.action("Actualizar recurso", () -> {
                    service.updateResource(resourceView.readResource(picker.resource(), service.categories()));
                    view.success("Recurso actualizado.");
                }),
                Option.action("Cambiar estado (mantenimiento / baja / disponible)", () -> {
                    Resource r = picker.resource();
                    ResourceStatus status = view.chooseEnum("Nuevo estado", new ResourceStatus[]{ResourceStatus.AVAILABLE,
                            ResourceStatus.MAINTENANCE, ResourceStatus.DECOMMISSIONED}, null);
                    service.changeStatus(r.id(), status);
                    view.success("Estado de " + r.code() + " cambiado a " + status + ".");
                }),
                Option.action("Eliminar recurso", () -> {
                    Resource r = picker.resource();
                    if (view.confirm("¿Confirma eliminar " + r.code() + " " + r.name() + "?")) {
                        service.deleteResource(r.id());
                        view.success("Recurso eliminado.");
                    }
                }),
                Option.action("Prestar recurso a un estudiante", () -> lend(BorrowerType.STUDENT)),
                Option.action("Prestar recurso a un profesor", () -> lend(BorrowerType.TEACHER)),
                Option.action("Registrar devolución", this::returnLoan),
                Option.action("Préstamos activos", () -> resourceView.showLoans(service.openLoans())),
                Option.action("Préstamos vencidos", this::overdueReport),
                Option.action("Historial de préstamos de un estudiante o profesor", () -> {
                    BorrowerType type = view.chooseEnum("Tipo de solicitante", BorrowerType.values(), BorrowerType.STUDENT);
                    int id = type == BorrowerType.STUDENT ? picker.student().id() : picker.teacher().id();
                    resourceView.showLoans(service.loansOf(type, id));
                }),
                Option.action("Historial de préstamos de un recurso", () ->
                        resourceView.showLoans(service.historyOfResource(picker.resource().id())))));
    }

    private void listInventory() {
        List<ResourceCategory> categories = service.categories();
        categories.forEach(c -> view.info("    " + c.id() + ") " + c.name()));
        Integer categoryId = view.readOptionalInt("Categoría", 1, Integer.MAX_VALUE);
        ResourceStatus status = view.chooseOptionalEnum("Estado", ResourceStatus.values());
        resourceView.showResources(service.listResources(categoryId, status));
    }

    private void lend(BorrowerType type) {
        int borrowerId = type == BorrowerType.STUDENT ? picker.student().id() : picker.teacher().id();
        Resource resource = picker.resource();
        LocalDate loanDate = view.readDate("Fecha de préstamo", LocalDate.now());
        LocalDate expected = view.readDate("Fecha de devolución prevista", loanDate.plusDays(type == BorrowerType.STUDENT ? 7 : 14));
        String notes = view.readOptional("Observaciones", null);
        int id = service.lend(type, borrowerId, resource.id(), loanDate, expected, notes);
        view.success("Préstamo #" + id + " registrado (" + type + "). " + resource.code() + " queda en estado Prestado.");
    }

    private void returnLoan() {
        List<Loan> open = service.openLoans();
        resourceView.showLoans(open);
        if (open.isEmpty()) {
            return;
        }
        BorrowerType type = view.chooseEnum("Tipo de préstamo", BorrowerType.values(), BorrowerType.STUDENT);
        int id = view.readInt("ID del préstamo", 1, Integer.MAX_VALUE);
        LocalDate date = view.readDate("Fecha real de devolución", LocalDate.now());
        String notes = view.readOptional("Observaciones (estado del recurso)", null);
        long late = service.returnLoan(type, id, date, notes);
        view.success("Devolución registrada; el recurso vuelve a estar Disponible.");
        if (late > 0) {
            view.warning("Se devolvió con " + late + " día(s) de retraso.");
        }
    }

    void overdueReport() {
        List<Loan> overdue = service.overdueLoans();
        resourceView.showLoans(overdue);
    }
}
