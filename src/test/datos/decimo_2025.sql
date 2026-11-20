-- Ejecutar en una base de demostración inicializada con docs/database.sql.
-- Los documentos DEMO-BUSCA identifican exclusivamente estos datos ficticios.
START TRANSACTION;

INSERT INTO estudiantes (tipo_documento, numero_documento, primer_nombre, segundo_nombre,
                         primer_apellido, segundo_apellido, fecha_nacimiento, genero, fecha_ingreso)
SELECT 'TI', d.documento, d.nombre, d.segundo_nombre, d.apellido, d.segundo_apellido,
       '2009-05-10', d.genero, '2025-01-20'
FROM (
    SELECT 'DEMO-BUSCA-01' AS documento, 'Juan' AS nombre, 'Carlos' AS segundo_nombre,
           'Pérez' AS apellido, 'González' AS segundo_apellido, 'M' AS genero
    UNION ALL SELECT 'DEMO-BUSCA-02', 'María', 'Elena', 'López', 'Ramírez', 'F'
    UNION ALL SELECT 'DEMO-BUSCA-03', 'Carlos', NULL, 'Gómez', NULL, 'M'
) d
WHERE NOT EXISTS (SELECT 1 FROM estudiantes e WHERE e.numero_documento = d.documento);

INSERT INTO matriculas (id_estudiante, id_grado, año_academico, estado_matricula, fecha_matricula)
SELECT e.id_estudiante, g.id_grado, 2025, 'Activa', '2025-01-20'
FROM estudiantes e
JOIN grados g ON g.nivel_numerico = 10
WHERE e.numero_documento IN ('DEMO-BUSCA-01', 'DEMO-BUSCA-02', 'DEMO-BUSCA-03')
  AND NOT EXISTS (SELECT 1 FROM matriculas m
                  WHERE m.id_estudiante = e.id_estudiante AND m.año_academico = 2025);

COMMIT;
