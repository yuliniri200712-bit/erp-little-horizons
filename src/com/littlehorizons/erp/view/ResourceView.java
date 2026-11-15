package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.enums.ResourceStatus;
import com.littlehorizons.erp.util.Formatter;

import java.time.LocalDate;
import java.util.List;

import static com.littlehorizons.erp.view.ConsoleView.row;

/** Forms and listings of inventory resources and loans. */
public class ResourceView {

    private final ConsoleView console;

    public ResourceView(ConsoleView console) {
        this.console = console;
    }

    public Resource readResource(Resource current, List<ResourceCategory> categories) {
        boolean isNew = current == null;
        console.section(isNew ? "Nuevo recurso" : "Actualizar recurso (Enter conserva el valor)");
        String code = console.readText("Código de inventario (ej: BIB-015)", isNew ? null : current.code());
        String name = console.readText("Nombre", isNew ? null : current.name());
        ResourceCategory category = isNew || console.confirm("¿Cambiar la categoría (actual: " + current.categoryName() + ")?")
                ? console.chooseFrom("Categoría", categories, ResourceCategory::name)
                : categories.stream().filter(c -> c.id() == current.categoryId()).findFirst().orElseThrow();
        String description = console.readOptional("Descripción", isNew ? null : current.description());
        LocalDate acquired = isNew ? console.readOptionalDate("Fecha de adquisición") : current.acquisitionDate();
        String location = console.readText("Ubicación física", isNew ? null : current.location());
        ResourceStatus status = isNew
                ? console.chooseEnum("Estado inicial", new ResourceStatus[]{ResourceStatus.AVAILABLE, ResourceStatus.MAINTENANCE}, ResourceStatus.AVAILABLE)
                : current.status();
        return new Resource(isNew ? null : current.id(), code, name, category.id(), category.name(), description, acquired, location, status);
    }

    public void showResources(List<Resource> resources) {
        console.table(List.of("ID", "Código", "Nombre", "Categoría", "Ubicación", "Estado"),
                resources.stream().map(r -> row(r.id(), r.code(), r.name(), r.categoryName(), r.location(), r.status())).toList());
    }

    public void showLoans(List<Loan> loans) {
        console.table(List.of("ID", "Tipo", "Solicitante", "Recurso", "Préstamo", "Devolución prevista", "Devolución real", "Estado", "Días vencido"),
                loans.stream().map(l -> row(l.id(), l.borrowerType(), l.borrowerName(), l.resourceCode() + " " + l.resourceName(),
                        Formatter.date(l.loanDate()), Formatter.date(l.expectedReturnDate()), Formatter.date(l.actualReturnDate()),
                        l.status(), l.daysOverdue() == 0 ? "-" : l.daysOverdue())).toList());
    }
}
