package test.modelo;

/** Datos de una matrícula para los resultados del buscador. */
public record EstudianteMatriculado(int id, String nombre, String tipoDocumento,
                                    String numeroDocumento, int idGrado) {
}
