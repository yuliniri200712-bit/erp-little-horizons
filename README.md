# ERP Little Horizons — Sistema de Gestión Educativa

Aplicación **Java de consola** con arquitectura **MVC**, persistencia en **MySQL 8.0+** (contenedor Docker) y
facturación en **PDF simulada**, para el *Colegio Internacional Little Horizons* (Preescolar a grado 11°).

## 1. Descripción, problema y funcionalidades

El colegio necesita centralizar en un solo sistema la información académica, extracurricular, financiera y de
recursos que hoy se maneja de forma dispersa. El ERP permite:

- Registrar estudiantes con sus acudientes y contactos de emergencia, matricularlos por grado y año y controlar su estado (Activo, Graduado, Retirado, Suspendido).
- Gestionar materias, inscripción por período, calificaciones (0.0–5.0 con un decimal), promedios e historial académico.
- Registrar asistencia diaria y detectar inasistencias recurrentes (más de 3 días de ausencia en una semana).
- Administrar el directorio de profesores, su asignación a materias por grupo y jornada, y horarios sin conflictos básicos.
- Gestionar clubes (con control de cupo) y eventos (participantes y puestos 1°, 2° y 3°).
- Manejar conceptos de cobro, becas/descuentos, cuentas por cobrar, pagos, reporte de mora y **factura PDF simulada** con CUFE, QR y resolución simulados.
- Controlar inventario de biblioteca y laboratorio, préstamos a estudiantes y profesores, devoluciones y vencidos.
- Consultar reportes y estadísticas desde un menú principal.

> **Aviso:** la factura es una **simulación académica**. No es una factura electrónica validada ni autorizada por la
> DIAN. El CUFE, el código QR y la resolución de habilitación son simulados y así se indica en el PDF.

## 2. Autores

| Nombre | Email | Contacto |
|---|---|---|
| `[Nombre del autor 1]` | `[correo@ejemplo.com]` | `[teléfono / GitHub]` |
| `[Nombre del autor 2]` | `[correo@ejemplo.com]` | `[teléfono / GitHub]` |

Instructor / trainer: `[nombre y usuario de GitHub del trainer]`

## 3. Tecnologías, IDE, base de datos y dependencias

| Elemento | Detalle |
|---|---|
| Lenguaje | Java (código compilado con `--release 21`; requiere **JDK 21 o superior**. Probado con JDK 27) |
| Build | Apache Maven (`pom.xml`). Se probó con el Maven incluido en IntelliJ IDEA |
| IDE | IntelliJ IDEA 2026.2 (cualquier IDE con soporte Maven sirve: abrir `pom.xml`) |
| Base de datos | MySQL **8.0** (imagen Docker oficial `mysql:8.0`; probado con 8.0.46) |
| Cliente SQL | DBeaver (opcional, para explorar la base) |
| Contenedores | Docker Desktop + `docker compose` |

Dependencias Maven:

| Dependencia | Versión | Uso |
|---|---|---|
| `com.mysql:mysql-connector-j` | 8.4.0 | Driver JDBC de MySQL |
| `com.github.librepdf:openpdf` | 3.0.5 | Generación de la factura PDF |
| `com.google.zxing:core` | 3.5.4 | Código QR de la factura |

Plugins: `maven-compiler-plugin` 3.14.1 y `maven-shade-plugin` 3.6.2 (genera un JAR ejecutable con dependencias).

## 4. Requisitos previos

- JDK 21+ (`java -version`).
- Maven 3.9+ (o el Maven integrado del IDE).
- Docker Desktop con `docker compose` (para MySQL **8.0 o superior**; versiones anteriores no soportan los `CHECK` del esquema).
- DBeaver (opcional).

## 5. Configuración de la base de datos

### 5.1 Crear el contenedor MySQL (Docker)

1. Copie el archivo de ejemplo y defina contraseñas propias:
   ```bash
   cp .env.example .env
   ```
   `.env` contiene `MYSQL_ROOT_PASSWORD`, `MYSQL_USER` (por defecto `lh_app`), `MYSQL_PASSWORD` y `MYSQL_PORT` (3306).
   **`.env` está en `.gitignore`: no se versiona.**
2. Levante MySQL:
   ```bash
   docker compose up -d
   ```
   En el **primer arranque** (volumen vacío) Docker ejecuta automáticamente `docs/database.sql` (DDL + DML), creando la
   base `erp_little_horizons` con datos de prueba. El usuario `lh_app` recibe permisos sobre esa base.
3. Para **reiniciar la base a los datos de prueba** (borra los datos actuales):
   ```bash
   docker compose down -v
   ```
   ```bash
   docker compose up -d
   ```

### 5.2 Credenciales de la aplicación y driver JDBC

1. Copie `config/db.properties.example` a `config/db.properties` (ignorado por git) y use el mismo usuario y contraseña de `.env`:
   ```properties
   db.url=jdbc:mysql://localhost:3306/erp_little_horizons?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota&characterEncoding=UTF-8
   db.user=lh_app
   db.password=<la misma MYSQL_PASSWORD de .env>
   ```
2. Alternativa: variables de entorno `LH_DB_URL`, `LH_DB_USER`, `LH_DB_PASSWORD` (tienen prioridad sobre el archivo).
3. El driver JDBC (`mysql-connector-j`) lo descarga Maven; no hay que instalarlo a mano.

`config/app.properties` (versionado, sin secretos) contiene los datos del emisor de la factura (ficticios), la
resolución simulada, la carpeta de facturas (`facturas/`) y el archivo de log (`error_log.txt`).

### 5.3 Conectar DBeaver

*Database → New Database Connection → MySQL* y complete:

| Campo | Valor |
|---|---|
| Server Host | `localhost` |
| Port | `3306` (o `MYSQL_PORT` de `.env`) |
| Database | `erp_little_horizons` |
| Username / Password | `lh_app` y `MYSQL_PASSWORD` de `.env` (o `root` y `MYSQL_ROOT_PASSWORD`) |
| Driver properties | `allowPublicKeyRetrieval=true`, `useSSL=false` |

Para recrear la base desde DBeaver, abra `docs/database.sql` en un editor SQL conectado como `root` y ejecútelo
completo (*Execute SQL Script*, Alt+X). El script **elimina y recrea** la base `erp_little_horizons`.

### 5.4 Esquema (`docs/database.sql`)

- **Sección DDL:** 27 tablas con PK, FK, índices y `CHECK`; nombres de tablas, campos y comentarios en español; notas en `DECIMAL(2,1)` con `CHECK (0.0–5.0)`; tablas separadas `prestamos_estudiante` y `prestamos_profesor`.
- **Sección DML:** datos ficticios y coherentes (fecha de referencia 15/11/2026). Conteos: `estudiantes` 38, `acudientes` 32, `profesores` 32, `materias` 48, `registros_academicos` 584, `pagos` 174, `clubes` 30, `recursos` 42 (además matrículas, inscripciones, asistencia, horarios, cobros, facturas, préstamos, etc.).
- **Origen:** se basa en el anexo del curso, guardado sin cambios en `docs/reference/script-bd-pry-erp-colegio-little-horizon.sql`. Las diferencias están justificadas en el encabezado de `docs/database.sql`:
  `horarios_clase` y `asignaciones_profesor.jornada` (horarios por grupo y turno), `contactos_emergencia`, `ajustes_estudiante` (becas/descuentos), `cargos` (cuentas por cobrar, necesarias para el reporte de mora), `consecutivos` (numeración de recibos y facturas), `conceptos_pago.porcentaje_iva`, `facturas.id_pago` único con subtotal/IVA/total, `pagos.metodo_pago` limitado a Efectivo | Transferencia | Tarjeta, `posicion_obtenida` como ENUM, tipo de documento `RC` y `CHECK` adicionales.

## 6. Instalación y ejecución

```bash
git clone <URL-del-repositorio-privado>
```
```bash
cd <carpeta-del-repositorio>
```

1. Configure `.env` y `config/db.properties` (sección 5) y ejecute `docker compose up -d`.
2. **Desde el IDE:** *File → Open* → seleccione `pom.xml` (abrir como proyecto) → ejecute la clase `com.littlehorizons.erp.Main`.
   El directorio de trabajo debe ser la raíz del proyecto (allí se leen `config/` y se escriben `facturas/` y `error_log.txt`).
3. **Desde la terminal:**
   ```bash
   mvn clean package
   ```
   ```bash
   java -jar target/erp-little-horizons-1.0.0.jar
   ```
   En Windows, si la consola muestra mal las tildes, ejecute antes `chcp 65001` o inicie con `-Dstdout.encoding=UTF-8`.
   La propiedad `-Dlh.console.encoding=<charset>` permite forzar la codificación de la entrada si fuera necesario.

### Pruebas de flujos principales (smoke tests)

`test/com/littlehorizons/erp/SmokeTest.java` ejecuta 56 verificaciones de reglas de negocio contra la base de datos
dentro de **una transacción que se revierte al final** (no modifica los datos):

```bash
mvn -q package
```
```bash
java -cp "target/erp-little-horizons-1.0.0.jar;target/test-classes" com.littlehorizons.erp.SmokeTest
```
(En Linux/macOS use `:` en lugar de `;` en el classpath.) Cubre: notas fuera de rango y con decimales incorrectos,
`CHECK` de la BD, nota sin inscripción o con profesor no asignado, cupo de club completo, membresía duplicada, doble
préstamo, devolución, préstamos vencidos, pagos (saldo, fecha futura, acudiente no vinculado), factura PDF, IVA,
becas, mora, conflictos de horario, acudiente obligatorio, matrícula duplicada, puestos de eventos y alertas de
inasistencia. Deja un PDF de prueba en `facturas/`.

## 7. Estructura y separación MVC

```text
src/com/littlehorizons/erp/
  Main.java              Punto de entrada: verifica conexión, construye controladores y abre el menú
  AppServices.java       Raíz de composición: crea DAOs e inyecta dependencias en los servicios
  config/                AppConfig (lee config/*.properties y variables de entorno)
  model/                 Entidades (records) + enums/ (valores ENUM de MySQL) + report/ (DTOs de reportes)
  dao/                   Acceso a datos JDBC (BaseDao, CrudDao y un DAO por agregado)
  service/               Reglas de negocio y casos de uso por módulo; invoice/ genera el PDF
  controller/            Coordinan casos de uso: menús, llamadas a servicios y a las vistas
  view/                  Entrada/salida de consola (ConsoleView, formularios y tablas por módulo)
  util/                  DatabaseConnection, TransactionManager, ErrorLogger, Validator, Formatter, CufeGenerator
  exception/             BusinessException, DataAccessException, OperationCancelledException
test/com/littlehorizons/erp/SmokeTest.java
docs/database.sql        DDL + DML
docs/reference/          Script original del anexo
config/                  app.properties, db.properties.example
docker-compose.yml, .env.example, pom.xml
```

- **Model:** solo datos (records inmutables y enums). No conocen la BD ni la consola.
- **View:** solo lectura/escritura en consola, validación de formato y reintento ante entradas inválidas. No contienen SQL ni reglas de negocio.
- **Controller:** arma los menús, pide datos a la vista, invoca servicios y muestra resultados/errores.
- **Service / DAO:** los servicios aplican las reglas; los DAOs ejecutan SQL con `PreparedStatement`.

## 8. Principios SOLID y patrones aplicados

**SOLID**

- **S – Responsabilidad única:** cada capa tiene un motivo de cambio: `StudentDao` (SQL de estudiantes), `StudentService` (reglas), `StudentView` (pantallas), `StudentController` (flujo). `ErrorLogger`, `Validator`, `Formatter` y `CufeGenerator` tienen una sola tarea.
- **O – Abierto/cerrado:** `LoanDao` construye su SQL a partir de `BorrowerType` (tabla y columna), así un nuevo tipo de solicitante solo requiere una constante del enum. `FinanceService` usa la interfaz `InvoiceGenerator`, por lo que otro formato de factura se agrega sin modificar el servicio. Los menús se definen como listas de `Option` en `BaseController`.
- **L – Sustitución de Liskov:** `MainController` trata a todos los módulos como `BaseController`; los DAOs principales implementan `CrudDao<T>` con el mismo contrato.
- **I – Segregación de interfaces:** `CrudDao` solo lo implementan las entidades con CRUD completo; catálogos de solo lectura (`GradeLevelDao`) no. `InvoiceGenerator` expone un único método.
- **D – Inversión de dependencias:** los servicios reciben sus dependencias por constructor (`AppServices`); `FinanceService` depende de la abstracción `InvoiceGenerator`, no de `PdfInvoiceGenerator`. *Limitación:* los servicios dependen de las clases DAO concretas (no de interfaces por DAO).

**Patrones**

- **DAO:** paquete `dao/`, con `BaseDao` como base común.
- **Singleton:** `DatabaseConnection` y `AppConfig` (instancia única, inicialización perezosa con doble verificación).
- **Template Method:** `BaseDao` (consulta/mapeo/errores) y `BaseController` (bucle de menú y manejo de errores).
- **Strategy:** `InvoiceGenerator` / `PdfInvoiceGenerator`.
- **Raíz de composición / inyección por constructor:** `AppServices` y `Main`.

**Lambdas y Stream API:** mapeadores `RowMapper` como lambdas en todos los DAOs; promedios por materia/período e
historial con `Collectors.groupingBy` (`AcademicService`); unión de préstamos de ambas tablas con `flatMap`
(`ResourceService`); totales con `reduce` (`FinanceService`); ordenamientos con `Comparator` (`ReportService`,
`ExtracurricularService`); filas de tablas con `map` en las vistas.

## 9. Funcionalidades por módulo (menú principal)

```text
ERP Colegio Little Horizons — Sistema de Gestión Educativa
1. Gestión de Estudiantes   2. Gestión Académica   3. Gestión de Profesores   4. Actividades Extracurriculares
5. Gestión Financiera       6. Gestión de Recursos 7. Reportes y Estadísticas 8. Salir
```

En cualquier selección de estudiante, profesor, materia o recurso se puede escribir el ID, el documento o código,
o presionar Enter para buscar por nombre; `0` cancela la operación. Cada submenú tiene la opción `0. Volver`.

1. **Gestión de Estudiantes:** listar (filtro por estado), buscar, ficha completa (acudientes, contactos, matrículas), registrar estudiante **con su acudiente obligatorio** (nuevo o existente) y matricular, actualizar, eliminar (bloqueado si tiene pagos), cambiar estado (el retiro cierra matrícula, inscripciones y clubes; la graduación exige matrícula en grado Once), matricular por grado y año (sin duplicados ni saltos de grado), listados y resumen de matrícula. Submenús de **acudientes** (CRUD, vincular/desvincular sin dejar al estudiante sin acudiente, ver hijos de un acudiente) y **contactos de emergencia**.
2. **Gestión Académica:** CRUD de materias por grado; inscripción por período (I–IV) en una o en todas las materias del grado (valida matrícula activa y grado); calificaciones 0.0–5.0 con **exactamente un decimal** (exige inscripción y profesor asignado; reemplazo con confirmación); promedio por materia, promedio general por período, promedio del curso por materia, ranking e historial académico completo; asistencia individual o por clase completa (`Presente | Ausente | Tardanza | Excusa Médica`), consulta por estudiante o clase, alerta de **más de 3 días de ausencia en una semana** e inasistencias recurrentes por materia.
3. **Gestión de Profesores:** directorio CRUD con especialización; asignación profesor–materia por año, sección y jornada (una sección no puede tener dos profesores para la misma materia); bloques de horario por día y hora con detección de cruces del **mismo profesor, mismo grupo o misma aula** y validación de la franja de la jornada; horario por profesor o por grupo.
4. **Actividades Extracurriculares:** clubes (CRUD, profesor encargado, inscripción con **validación de cupo** bloqueando la fila del club, retiro con fecha y motivo, miembros); eventos (CRUD, tipo, participantes, rol, puestos `1er lugar | 2do lugar | 3er lugar | Participante` únicos por evento y solo si hay competencia); actividades por estudiante.
5. **Gestión Financiera:** registrar pago (contra un cobro pendiente o por concepto; recibo consecutivo `RC-000001`; método `Efectivo | Transferencia | Tarjeta`; acudiente pagador vinculado) que **al confirmarse genera la factura PDF**; historial por estudiante y por acudiente (todos sus hijos); reporte de **deudas con más de un mes de atraso**; cobros individuales y masivos; anulación de cobros; conceptos de pago; becas y descuentos (porcentaje sobre el valor base, tope 100 %); listado de facturas, regeneración de PDF, edición y anulación de pagos.
6. **Gestión de Recursos:** inventario con código único, categoría y estado; búsqueda y filtros; préstamo a **estudiante o profesor** (solo si está `Disponible`; máximo 3 préstamos abiertos por estudiante y ninguno vencido); devolución con fecha real que deja el recurso `Disponible`; los préstamos con fecha prevista pasada se marcan `Vencido` automáticamente; historial por persona y por recurso.
7. **Reportes y Estadísticas:** resumen general; ranking de promedios; promedio del curso por materia; historial académico; alertas de inasistencia y recurrentes; ocupación de clubes; participación y podios en eventos; participación por estudiante; mora > 1 mes; préstamos vencidos.

### Factura PDF (simulada)

Se genera al confirmar un pago en `facturas/factura_NNNNNN.pdf` (la ruta se muestra en pantalla y queda en
`facturas.ruta_archivo`). Incluye emisor (nombre, NIT, dirección, teléfono y correo de `config/app.properties`,
datos ficticios), adquiriente (acudiente responsable: nombre, documento, dirección), estudiante y grado, consecutivo,
fechas, recibo, medio de pago, línea (descripción, cantidad, precio unitario, subtotal), subtotal, IVA si el concepto
lo tiene (p. ej. uniforme 19 %; servicios educativos 0 %) y total. Elementos **simulados**: CUFE = SHA-384 de
`número|NIT|fecha|total` (no es el algoritmo de la DIAN), QR con los datos de la factura y resolución de habilitación.

## 10. Excepciones y logging

- `BusinessException`: violación de reglas o validaciones; se muestra al usuario y se repite la operación o se vuelve al menú.
- `DataAccessException`: envuelve `SQLException` y traduce códigos MySQL (duplicado, FK, CHECK, acceso denegado, sin conexión) a mensajes en español.
- `OperationCancelledException`: el usuario cancela con `0`.
- Todo el JDBC usa `PreparedStatement` y `try-with-resources`; las operaciones compuestas (registrar estudiante con acudiente, pagos con factura, préstamos y devoluciones, inscripción a clubes, cobros masivos) usan `TransactionManager` (commit/rollback).
- **`error_log.txt`** se crea en el directorio de trabajo (configurable con `log.file`). **Política:** se registran errores técnicos (SQL, conexión, E/S del PDF, errores inesperados) con fecha, nivel, contexto y traza completa; las validaciones de negocio **no** se registran porque son esperadas. El archivo está en `.gitignore`.

## 11. Licencia y contacto

Proyecto académico. Licencia: `[por definir por el equipo]`. Contacto: `[correo del equipo]`.

## 12. Limitaciones y mejoras futuras

- No hay autenticación ni roles de usuario.
- La factura **no** es electrónica real: no se firma ni se envía a la DIAN.
- Los datos de prueba están fechados alrededor del 15/11/2026; los reportes dependientes de la fecha actual (mora, vencidos, alertas) mostrarán resultados distintos si se ejecutan en otra fecha.
- La nota "Final" se registra manualmente; no se calcula automáticamente a partir de los períodos.
- Las pruebas son *smoke tests* de integración en Java plano (sin JUnit) y requieren la base de datos en ejecución.
- Mejoras posibles: interfaces por DAO, pruebas unitarias con JUnit, pool de conexiones, exportación de reportes a PDF/Excel.
