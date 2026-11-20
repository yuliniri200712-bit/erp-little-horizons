package test.controlador;

import com.littlehorizons.erp.dao.jdbc.BaseDao;
import com.littlehorizons.erp.dao.jdbc.JdbcGradeLevelDao;
import com.littlehorizons.erp.model.GradeLevel;
import com.littlehorizons.erp.model.enums.EnrollmentStatus;
import test.modelo.EstudianteMatriculado;

import java.util.List;
import java.util.stream.Collectors;

public final class BuscadorEstudiantes extends BaseDao {
    public static final int ANIO_ACADEMICO = 2025;

    public List<GradeLevel> gradosDisponibles() {
        return new JdbcGradeLevelDao().findAll();
    }

    public List<EstudianteMatriculado> buscarPorGrado(GradeLevel grado) {
        String sql = """
                SELECT e.id_estudiante, %s AS nombre, e.tipo_documento, e.numero_documento, g.id_grado
                FROM matriculas m
                JOIN estudiantes e ON e.id_estudiante = m.id_estudiante
                JOIN grados g ON g.id_grado = m.id_grado
                WHERE m.año_academico = ? AND m.estado_matricula = ?
                ORDER BY e.id_estudiante
                """.formatted(fullNameSql("e"));
        List<EstudianteMatriculado> matriculados = query(sql, rs -> new EstudianteMatriculado(
                rs.getInt("id_estudiante"), rs.getString("nombre").strip().replaceAll("\\s+", " "),
                rs.getString("tipo_documento"), rs.getString("numero_documento"), rs.getInt("id_grado")),
                ANIO_ACADEMICO, EnrollmentStatus.ACTIVE);
        return matriculados.stream()
                .filter(estudiante -> estudiante.idGrado() == grado.id())
                .collect(Collectors.toList());
    }
}
