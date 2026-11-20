package test.vista;

import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.GradeLevel;
import com.littlehorizons.erp.util.ErrorLogger;
import test.controlador.BuscadorEstudiantes;
import test.modelo.EstudianteMatriculado;

import java.util.List;
import java.util.Scanner;

public final class BuscadorEstudiantesMain {
    private static final String SEPARADOR = "=".repeat(79);
    private static final String LINEA = "-".repeat(79);
    private final BuscadorEstudiantes buscador = new BuscadorEstudiantes();
    private final Scanner entrada = new Scanner(System.in);

    public static void main(String[] args) {
        new BuscadorEstudiantesMain().ejecutar();
    }

    private void ejecutar() {
        while (true) {
            System.out.println(SEPARADOR);
            System.out.println("BUSCADOR AVANZADO DE ESTUDIANTES - PROTOTIPO");
            System.out.println(SEPARADOR);
            System.out.println("""
                    Seleccione el filtro que desea aplicar:
                    1. Filtrar por grado académico [FUNCIONAL]
                    2. Filtrar por rango de edad [NO IMPLEMENTADO]
                    3. Filtrar por género [NO IMPLEMENTADO]
                    4. Filtrar por estado de matrícula [NO IMPLEMENTADO]
                    5. Filtrar por rango de promedio académico [NO IMPLEMENTADO]
                    0. Salir""");
            int opcion = leerOpcion("Seleccione una opción: ", 5);
            switch (opcion) {
                case 0 -> {
                    System.out.println("Saliendo del buscador...\nGracias por usar el sistema.");
                    return;
                }
                case 1 -> {
                    try {
                        seleccionarGrado();
                    } catch (DataAccessException e) {
                        ErrorLogger.error("Buscador de estudiantes", e);
                        System.out.println(e.getUserMessage());
                        pausa("volver al menú principal");
                    }
                }
                default -> {
                    System.out.println("Esta funcionalidad aún no ha sido implementada.");
                    System.out.println("Será agregada en futuras versiones del sistema.");
                    pausa("continuar");
                }
            }
        }
    }

    private void seleccionarGrado() {
        System.out.println("--- FILTRAR POR GRADO ACADÉMICO ---");
        List<GradeLevel> grados = buscador.gradosDisponibles();
        if (grados.isEmpty()) {
            System.out.println("El catálogo de grados está vacío.");
        } else {
            System.out.println("Grados disponibles en el sistema:");
            for (int i = 0; i < grados.size(); i++) {
                System.out.printf("%d. %s%n", i + 1, grados.get(i).name());
            }
            int opcion = leerOpcion("Ingrese el número del grado que desea filtrar (o 0 para cancelar): ",
                    grados.size());
            if (opcion == 0) {
                System.out.println("Operación cancelada.");
            } else {
                GradeLevel grado = grados.get(opcion - 1);
                System.out.printf("Consultando estudiantes matriculados en %s...%n", grado.name());
                mostrarResultados(grado, buscador.buscarPorGrado(grado));
            }
        }
        pausa("volver al menú principal");
    }

    private static void mostrarResultados(GradeLevel grado, List<EstudianteMatriculado> resultados) {
        System.out.println(SEPARADOR);
        System.out.println("RESULTADOS DE LA BÚSQUEDA");
        System.out.println(SEPARADOR);
        System.out.printf("Filtro aplicado: Grado académico = %s%n%n", grado.name());
        int anio = BuscadorEstudiantes.ANIO_ACADEMICO;
        if (resultados.isEmpty()) {
            System.out.printf("No se encontraron estudiantes matriculados en %s para el año %d.%n", grado.name(), anio);
            System.out.printf("Verifique que haya matrículas activas de ese grado en %d.%n", anio);
            return;
        }
        int total = resultados.size();
        String cantidad = total == 1 ? "1 estudiante" : total + " estudiantes";
        String resumen = total == 1 ? "Se encontró 1 estudiante matriculado" : "Se encontraron " + cantidad + " matriculados";
        System.out.printf("%s en %s (año %d):%n", resumen, grado.name(), anio);
        for (EstudianteMatriculado estudiante : resultados) {
            System.out.println(LINEA);
            System.out.printf("ID: %d%nNombre: %s%nDocumento: %s %s%n",
                    estudiante.id(), estudiante.nombre(), estudiante.tipoDocumento(), estudiante.numeroDocumento());
        }
        System.out.println(LINEA);
        System.out.println("Total de resultados: " + cantidad);
    }

    private int leerOpcion(String mensaje, int maximo) {
        while (true) {
            System.out.print(mensaje);
            if (!entrada.hasNextLine()) return 0;
            try {
                int numero = Integer.parseInt(entrada.nextLine().strip());
                if (numero >= 0 && numero <= maximo) return numero;
                System.out.printf("Error: Debe seleccionar un número entre 1 y %d (o 0).%n", maximo);
            } catch (NumberFormatException e) {
                System.out.println("Error: Debe ingresar un número válido.");
            }
            pausa("intentar nuevamente");
        }
    }

    private void pausa(String accion) {
        System.out.printf("Presione Enter para %s...%n", accion);
        if (entrada.hasNextLine()) entrada.nextLine();
    }
}
