# ERP Colegio Little Horizons

## Tabla de contenido

1. [Título del Proyecto](#1-título-del-proyecto)
2. [Descripción](#2-descripción)
3. [Autores](#3-autores)
4. [Tecnologías Utilizadas](#4-tecnologías-utilizadas)
5. [Requerimientos Previos](#5-requerimientos-previos)
6. [Configuración de la Base de Datos](#6-configuración-de-la-base-de-datos)
7. [Instalación y Ejecución](#7-instalación-y-ejecución)
8. [Estructura del Proyecto](#8-estructura-del-proyecto)
9. [Principios y Patrones de Diseño Aplicados](#9-principios-y-patrones-de-diseño-aplicados)
10. [Funcionalidades Principales](#10-funcionalidades-principales)
11. [Manejo de Errores y Logs](#11-manejo-de-errores-y-logs)
12. [Capturas de Pantalla](#12-capturas-de-pantalla)
13. [Problemas Conocidos y Limitaciones](#13-problemas-conocidos-y-limitaciones)
14. [Futuras Mejoras](#14-futuras-mejoras)
15. [Licencia](#15-licencia)
16. [Contacto](#16-contacto)

---

## 1. Título del Proyecto

**ERP Colegio Little Horizons — Sistema de Gestión Educativa** (`erp-little-horizons`, versión `1.0.0`).

Aplicación Java de consola con arquitectura MVC, persistencia en MySQL 8.0+ y facturación electrónica simulada (TXT y
PDF), para el
*Colegio Internacional Little Horizons* (Preescolar a grado 11°).

---

## 2. Descripción

### Resumen del propósito

El colegio atiende estudiantes desde preescolar hasta 11° y maneja su información en cuadernos y hojas de cálculo
dispersas. Este ERP centraliza en una sola aplicación y una sola base de datos los procesos académicos,
extracurriculares, financieros y de recursos, para que las decisiones se tomen con datos confiables.

### Problemas que resuelve

| Problema | Cómo lo resuelve el sistema |
|---|---|
| Pérdida de información académica (notas en cuadernos) | Notas registradas en MySQL por estudiante, materia y período, con promedios automáticos e historial completo. |
| Desorganización en actividades extracurriculares | Catálogo de clubes con control de cupo, membresías con fecha y estado, eventos con participantes y puestos. |
| Caos en la facturación | Cada pago confirmado emite su factura electrónica (TXT y PDF) con consecutivo, IVA, CUFE, QR y resolución simulados; también se pueden agrupar varios pagos en una factura. |
| Recursos que se "pierden" | Préstamos a estudiantes y profesores, devoluciones y reporte de préstamos vencidos. |
| Poca visibilidad del rendimiento | Promedios por materia, por período y por curso, ranking, alertas de inasistencia y reporte de morosos. |

### Funcionalidades principales

- Registro de estudiantes con acudientes (uno o varios; un acudiente puede tener varios hijos) y contactos de emergencia.
- Matrícula por grado y año con estado (Activo, Graduado, Retirado) e inscripción de materias por período.
- Directorio de profesores, asignación de materias por grupo y jornada, y horarios sin cruces.
- Calificaciones 0.0–5.0 con un decimal, promedios automáticos e historial académico.
- Asistencia diaria por materia y reporte de estudiantes con más de 3 ausencias en una semana.
- Clubes con validación de cupo y eventos con participantes y puestos (1er, 2do, 3er lugar, Participante).
- Conceptos de pago con becas y descuentos, cobros, pagos, historial por estudiante y por acudiente, reporte de mora.
- Factura electrónica **simulada** emitida automáticamente al confirmar un pago, o con varios pagos a la vez; se
  muestra en consola y se guarda en TXT y en PDF.
- Inventario de biblioteca y laboratorio, préstamos, devoluciones y vencidos.
- Reportes y estadísticas, con exportación a archivos CSV.

> **Aviso:** la factura es una **simulación académica**. No es una factura electrónica validada ni autorizada por la
> DIAN. El CUFE, el código QR y la resolución de habilitación son simulados y así se indica en el TXT y en el PDF.

---

## 3. Autores

| | |
|---|---|
| **Nombre completo** | Andrés Julián Rolón Ibero |
| **Correo electrónico** | yuliniri200712@gmail.com |
| **GitHub** | [yuliniri200712-bit](https://github.com/yuliniri200712-bit) |
| **Rol** | Diseño y desarrollo del sistema (modelo, vistas, controladores, servicios, base de datos y pruebas) |

Proyecto desarrollado como entrega académica.

---

## 4. Tecnologías Utilizadas

| Categoría | Tecnología |
|---|---|
| Lenguaje de programación | Java (compilado con `--release 21`; requiere **JDK 21 o superior**; probado con JDK 27) |
| IDE utilizado | IntelliJ IDEA 2026.2 (incluye configuración de ejecución compartida en `.run/`) |
| Herramienta de construcción | Apache Maven (`pom.xml`) |
| Base de datos | MySQL **8.0** (imagen Docker oficial `mysql:8.0`; probado con 8.0.46) |
| Contenedores (opcional) | Docker Desktop con `docker compose` |
| Cliente SQL (opcional) | DBeaver |

**Librerías externas** (Maven las descarga automáticamente):

| Dependencia | Versión | Uso |
|---|---|---|
| `com.mysql:mysql-connector-j` | 8.4.0 | Driver JDBC de MySQL |
| `com.github.librepdf:openpdf` | 3.0.5 | Generación de la factura en PDF |
| `com.google.zxing:core` | 3.5.4 | Código QR de la factura |

Plugins de Maven: `maven-compiler-plugin` 3.14.1 y `maven-shade-plugin` 3.6.2 (genera un JAR ejecutable con las
dependencias incluidas).

---

## 5. Requerimientos Previos

| Software | Versión | Para qué |
|---|---|---|
| JDK | **21 o superior** | Compilar y ejecutar |
| Maven | 3.9 o superior (IntelliJ IDEA lo trae incluido) | Descargar dependencias y compilar |
| **MySQL Server** | **8.0 o superior (obligatorio)** | Base de datos. Las restricciones `CHECK` del esquema solo funcionan desde MySQL 8.0. Puede ser una instalación local o el contenedor Docker del proyecto. |
| Docker Desktop | Reciente (opcional) | Levantar MySQL 8.0 con un comando |
| Git | Reciente | Clonar el repositorio |
| IntelliJ IDEA | Community o Ultimate | IDE recomendado |

---

## 6. Configuración de la Base de Datos

### 6.1. Creación de la base de datos

El archivo [`docs/database.sql`](docs/database.sql) crea la base `erp_little_horizons` desde cero. Tiene dos
secciones claramente separadas:

- **Sección 1 – DDL:** 28 tablas con llaves primarias, llaves foráneas, índices y restricciones `CHECK`; nombres de
  tablas, campos y comentarios en español; notas en `DECIMAL(2,1)` con `CHECK (0.0–5.0)`; tablas separadas
  `prestamos_estudiante` y `prestamos_profesor`. Está basado en el script del anexo (guardado sin cambios en
  [`docs/reference/`](docs/reference/)); cada diferencia está justificada en el encabezado del script.
- **Sección 2 – DML:** datos de prueba ficticios y coherentes (fecha de referencia 15/11/2026):

| Tabla | Registros |
|---|---|
| `estudiantes` | 38 |
| `acudientes` | 32 |
| `profesores` | 32 |
| `materias` | 48 |
| `registros_academicos` | 584 |
| `pagos` | 174 |
| `clubes` | 30 |
| `recursos` | 42 |

Además incluye matrículas, inscripciones, asistencias, horarios, membresías, eventos, cobros, facturas (13, una de
ellas con dos pagos y IVA del 19 %) con su detalle y préstamos.

**Opción A – Docker (recomendada):**

1. Copie el archivo de ejemplo y defina sus propias contraseñas (`.env` no se sube a git):
   ```bash
   cp .env.example .env
   ```
2. Levante MySQL. En el primer arranque se ejecuta automáticamente `docs/database.sql` (DDL + DML):
   ```bash
   docker compose up -d
   ```
3. Para reiniciar la base con los datos de prueba (borra los datos actuales):
   ```bash
   docker compose down -v
   ```
   ```bash
   docker compose up -d
   ```

**Opción B – MySQL local o DBeaver:** conéctese como `root` a un MySQL 8.0+, abra `docs/database.sql` y ejecútelo
completo (en DBeaver: *Execute SQL Script*, Alt+X). El script **elimina y vuelve a crear** la base
`erp_little_horizons`.

Conexión desde DBeaver (*Database → New Database Connection → MySQL*): host `localhost`, puerto `3306`, base
`erp_little_horizons`, usuario y contraseña de `.env`, y en *Driver properties* `allowPublicKeyRetrieval=true` y
`useSSL=false`.

### 6.2. Configuración de credenciales

Las credenciales **no** están en el código ni en el repositorio:

1. Copie `config/db.properties.example` a `config/db.properties` (ignorado por git) y escriba su usuario y contraseña
   (si usa Docker, los mismos de `.env`):
   ```properties
   db.url=jdbc:mysql://localhost:3306/erp_little_horizons?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota&characterEncoding=UTF-8
   db.user=lh_app
   db.password=<su contraseña>
   ```
2. Alternativa: variables de entorno `LH_DB_URL`, `LH_DB_USER` y `LH_DB_PASSWORD` (tienen prioridad sobre el archivo).

`config/app.properties` (sin secretos) guarda los datos ficticios del emisor de la factura, la resolución simulada
(`invoice.resolution.*`: número, fecha, meses de vigencia y rango autorizado; `invoice.technical.key`) y las carpetas
de salida (`facturas/`, `reportes/`) y del log (`error_log.txt`).

### 6.3. Instalación del driver JDBC

No hay que instalar nada a mano: el driver `mysql-connector-j` 8.4.0 está declarado en `pom.xml` y Maven lo descarga
al abrir o compilar el proyecto. Desde JDBC 4 el driver se registra solo (no se necesita `Class.forName`). Si
IntelliJ no lo encuentra, recargue Maven (panel *Maven* → *Reload All Maven Projects*).

---

## 7. Instalación y Ejecución

### 7.1. Clonar el repositorio

```bash
git clone https://github.com/yuliniri200712-bit/erp-little-horizons.git
```
```bash
cd erp-little-horizons
```

Luego configure la base de datos y las credenciales (sección 6).

### 7.2. Abrir el proyecto en el IDE

En IntelliJ IDEA: *File → Open* → seleccione la carpeta del proyecto. IntelliJ detecta el `pom.xml` e importa el
proyecto y sus dependencias automáticamente.

### 7.3. Compilar

Desde IntelliJ: *Build → Build Project*. Desde la terminal:

```bash
mvn clean package
```

Genera `target/erp-little-horizons-1.0.0.jar`, un JAR ejecutable con todas las dependencias.

### 7.4. Ejecutar

- **IntelliJ IDEA:** en la barra superior elija la configuración **ERP Little Horizons** (archivo
  `.run/ERP Little Horizons.run.xml`) y pulse **Run**. Ya fija la clase `com.littlehorizons.erp.Main`, el directorio
  de trabajo en la raíz del proyecto y la codificación UTF-8 (tildes y ñ en la consola del IDE).
- **Terminal:**
  ```bash
  java -jar target/erp-little-horizons-1.0.0.jar
  ```
  En Windows, si la consola muestra mal las tildes, ejecute antes `chcp 65001`.

Al iniciar, el programa verifica la conexión con MySQL y muestra el menú principal. Si no puede conectarse, muestra un
mensaje claro y deja el detalle técnico en `error_log.txt`.

### 7.5. Pruebas de los flujos principales

`test/com/littlehorizons/erp/SmokeTest.java` ejecuta 74 verificaciones de reglas de negocio contra la base de datos,
dentro de **una transacción que se revierte al final** (no modifica los datos):

```bash
mvn -q package
```
```bash
java -cp "target/erp-little-horizons-1.0.0.jar;target/test-classes" com.littlehorizons.erp.SmokeTest
```

(En Linux/macOS use `:` en lugar de `;`.) Cubre notas fuera de rango y con decimales incorrectos, `CHECK` de la base
de datos, cupo de club completo, membresía duplicada, doble préstamo, devoluciones, vencidos, pagos (saldo, fecha
futura, acudiente no vinculado), factura automática y con varios pagos (TXT y PDF, CUFE, pago ya facturado,
cliente no vinculado, resolución vencida, rango agotado), IVA, becas, mora, conflictos de horario, acudiente obligatorio,
matrícula duplicada, puestos de eventos, alertas de inasistencia y exportación CSV.

---

## 8. Estructura del Proyecto

### Arquitectura MVC

```text
Usuario ─ teclado ─▶ VISTA ─▶ CONTROLADOR ─▶ SERVICIO (reglas) ─▶ DAO (interfaz) ─▶ Jdbc*Dao ─▶ MySQL
Usuario ◀ pantalla ─ VISTA ◀─ CONTROLADOR ◀─ SERVICIO          ◀─ entidades (records)
```

- **Modelo (`model/`):** entidades del dominio como `record` inmutables, enums con los valores ENUM de MySQL y DTOs.
  No conocen la base de datos ni la consola.
- **Vista (`view/`):** solo lectura y escritura en consola: menús, formularios, tablas, validación de formato y
  reintento ante entradas inválidas. No contienen SQL ni reglas de negocio.
- **Controlador (`controller/`):** arma los menús, pide los datos a la vista, invoca los servicios y muestra los
  resultados o errores.
- **Servicios (`service/`) y DAO (`dao/`):** los servicios aplican las reglas de negocio; los DAO ejecutan el SQL.

### Árbol del proyecto

```text
src/com/littlehorizons/erp/
├── Main.java                Punto de entrada: verifica la conexión, crea los controladores y abre el menú
├── AppServices.java         Raíz de composición: crea los DAO JDBC e inyecta dependencias en los servicios
├── config/                  AppConfig (lee config/*.properties y variables de entorno)
├── model/                   Entidades (records)
│   ├── enums/               Valores de las columnas ENUM (en español en la BD, constantes en inglés)
│   ├── report/              Datos de reportes (promedios, mora, alertas, historial...)
│   └── dto/                 Entrada/salida de casos de uso (PaymentRequest, PaymentResult...)
├── dao/                     Interfaces DAO (StudentDao, PaymentDao...) y CrudDao<T>
│   └── jdbc/                Implementaciones JDBC (JdbcStudentDao...) y BaseDao
├── service/                 Reglas de negocio por módulo
│   └── invoice/             InvoiceGenerator (TXT y PDF), InvoiceTextRenderer e InvoiceQr
├── controller/              Un controlador por módulo + EntityPicker (búsqueda de registros)
├── view/                    ConsoleView (E/S genérica) y una vista por módulo
├── util/                    DatabaseConnection, TransactionManager, ErrorLogger, Validator, Formatter,
│                            CufeGenerator, CsvWriter, SelectionParser
└── exception/               BusinessException, DataAccessException, OperationCancelledException,
                             MainMenuRequestedException
test/com/littlehorizons/erp/SmokeTest.java
docs/database.sql            DDL + DML
docs/reference/              Script original del anexo
docs/ejemplos/               Factura de ejemplo generada por el sistema (TXT y PDF)
config/                      app.properties y db.properties.example
.run/                        Configuración de ejecución de IntelliJ IDEA
docker-compose.yml, .env.example, pom.xml, LICENSE
```

### Paquetes principales

| Paquete | Responsabilidad |
|---|---|
| `model` | Datos del dominio, sin lógica de acceso a datos ni de consola |
| `view` | Interacción con el usuario por consola |
| `controller` | Flujo de cada módulo (menú → servicio → vista) |
| `service` | Reglas de negocio y cálculos (promedios, cupos, mora, préstamos, facturación) |
| `dao` / `dao.jdbc` | Contratos de acceso a datos y su implementación con JDBC |
| `util` | Conexión, transacciones, log, validaciones, formatos, CUFE y CSV |
| `config` | Lectura de la configuración |
| `exception` | Excepciones propias de la aplicación |

Todo el código (clases, métodos, variables y comentarios) está en inglés y sigue las convenciones de Java:
`PascalCase` para clases, `camelCase` para métodos y variables, `UPPER_SNAKE_CASE` para constantes. La interfaz de
usuario y los datos de la base están en español.

---

## 9. Principios y Patrones de Diseño Aplicados

### 9.1. Principios SOLID

- **S – Responsabilidad única:** cada clase tiene un solo motivo de cambio. Por ejemplo, para estudiantes:
  `JdbcStudentDao` (SQL), `StudentService` (reglas), `StudentView` (pantallas) y `StudentController` (flujo).
  `ErrorLogger`, `Validator`, `Formatter`, `CufeGenerator` y `CsvWriter` hacen una sola tarea cada uno.
- **O – Abierto/cerrado:** `JdbcLoanDao` construye su SQL a partir de `BorrowerType` (tabla y columna), así un nuevo
  tipo de solicitante solo requiere una constante en el enum. `FinanceService` recibe una lista de
  `InvoiceGenerator` (hoy TXT y PDF), por lo que se puede agregar otro formato de factura sin modificar el servicio.
- **L – Sustitución de Liskov:** `MainController` trata a todos los módulos como `BaseController`; cualquier
  implementación de una interfaz DAO puede reemplazar a otra sin cambiar los servicios.
- **I – Segregación de interfaces:** una interfaz por conjunto de datos (`StudentDao`, `LoanDao`, `ClubDao`...); solo
  las entidades con CRUD completo extienden `CrudDao<T>` y los catálogos de solo lectura (`GradeLevelDao`) no.
  `InvoiceGenerator` expone solo lo que necesita el servicio (formato, ruta y generar).
- **D – Inversión de dependencias:** los servicios dependen de **interfaces** (`StudentDao`, `InvoiceGenerator`...) y
  las reciben por constructor. Solo `AppServices` conoce las clases concretas (`JdbcStudentDao`,
  `TxtInvoiceGenerator`, `PdfInvoiceGenerator`).

### 9.2. Patrones de Diseño

| Patrón | Dónde | Justificación |
|---|---|---|
| **DAO** | `dao` (interfaces) y `dao.jdbc` (implementaciones) | Aísla todo el SQL; los servicios no saben que hay una base relacional detrás. |
| **Singleton** | `DatabaseConnection`, `AppConfig` | Una sola configuración y un solo punto de acceso a la conexión (inicialización perezosa con doble verificación). |
| **Template Method** | `BaseDao`, `BaseController` | El ciclo "consulta → mapeo → manejo de errores" y el ciclo "menú → acción → manejo de errores" se escriben una sola vez. |
| **Strategy** | `InvoiceGenerator` / `TxtInvoiceGenerator`, `PdfInvoiceGenerator` | Cada formato de la factura es una estrategia; se agregan o quitan sin tocar `FinanceService`. |
| **Inyección por constructor / raíz de composición** | `AppServices`, `Main` | Todas las dependencias se crean en un solo lugar. |

**Lambdas y Stream API:** los `RowMapper` de todos los DAO son lambdas; `AcademicService` calcula promedios por
materia y período con `Collectors.groupingBy`; `ResourceService` une los préstamos de ambas tablas con `flatMap`;
`FinanceService` suma con `reduce` y arma las líneas de la factura con `map` y `Collectors.toMap`; `InvoiceTextRenderer` agrupa el IVA por tarifa con `groupingBy`; `ReportService` y `ExtracurricularService` ordenan con `Comparator`; las vistas
construyen las tablas con `map`; `ConsoleView.readValidated` recibe el validador como lambda.

---

## 10. Funcionalidades Principales

### Navegación

- Menú principal por módulos (opciones 1 a 8) y submenús con la opción `0. Volver`.
- Escribir **`inicio`** en cualquier pregunta regresa directamente al menú principal.
- En la selección de estudiantes, profesores, materias o recursos se puede escribir el ID, el documento o código, o
  presionar Enter para buscar por nombre; `0` cancela la operación.
- Toda entrada se valida: si un dato es inválido se muestra el motivo y se vuelve a preguntar.

### 1. Gestión de Estudiantes

Listar (con filtro por estado), buscar, ficha completa, registrar estudiante **con su acudiente obligatorio** (nuevo o
existente), actualizar, eliminar (bloqueado si tiene pagos), cambiar estado (el retiro cierra matrícula, inscripciones y
clubes; la graduación exige matrícula en grado Once), matricular por grado y año (sin duplicados ni saltos de grado),
listados y resumen de matrícula. Submenús de **acudientes** (CRUD, vincular y desvincular sin dejar al estudiante sin
acudiente, ver los hijos de un acudiente) y **contactos de emergencia**.

### 2. Gestión Académica

CRUD de materias por grado; inscripción por período (I–IV) en una o en todas las materias del grado; calificaciones
0.0–5.0 con **exactamente un decimal** (exige inscripción y profesor asignado); promedio por materia, promedio general
por período, promedio del curso por materia, ranking e historial académico completo; asistencia individual o por clase
completa (`Presente | Ausente | Tardanza | Excusa Médica`), alerta de **más de 3 días de ausencia en una semana** e
inasistencias recurrentes por materia.

### 3. Gestión de Profesores

Directorio CRUD con especialización; asignación profesor–materia por año, sección y jornada; bloques de horario por día
y hora con detección de cruces del **mismo profesor, mismo grupo o misma aula** y validación de la franja de la
jornada; horario por profesor o por grupo.

### 4. Actividades Extracurriculares

Clubes (CRUD, profesor encargado, inscripción con **validación de cupo**, retiro con fecha y motivo, miembros) y eventos
(CRUD, tipo, participantes, rol, puestos `1er lugar | 2do lugar | 3er lugar | Participante` únicos por evento y solo si
hay competencia).

### 5. Gestión Financiera

Registrar pago (contra un cobro pendiente o por concepto; recibo consecutivo; método `Efectivo | Transferencia |
Tarjeta`) que **al confirmarse emite su factura electrónica**; historial por estudiante y por acudiente (todos sus
hijos); reporte de **deudas con más de un mes de atraso**; cobros individuales y masivos; conceptos de pago; becas y
descuentos; corrección y anulación de pagos (un pago ya facturado no se anula: se haría con una nota crédito).

**Facturas electrónicas (simuladas).** Submenú *Gestión Financiera > Facturas electrónicas*:

| Opción | Qué hace |
|---|---|
| Emitir factura de pagos sin facturar | Lista los pagos del estudiante que aún no tienen factura; se eligen con `1,3` o `todos` (máximo 30). Sugiere como cliente al acudiente que pagó (o al responsable financiero) y permite elegir otro acudiente del estudiante. Pide confirmación, muestra la factura completa en la consola y la guarda en TXT y PDF. |
| Ver una factura por su número | Acepta `13` o `FE-000013` y muestra la factura en la consola, igual que el TXT. |
| Facturas de un estudiante / Listar todas | Número, fecha y hora, estudiante, cliente, cantidad de pagos, subtotal, IVA y total. |
| Generar de nuevo los archivos | Vuelve a escribir el TXT y el PDF con los datos guardados (mismo contenido). |

Reglas: cada pago se factura **una sola vez** (`detalle_factura.id_pago` es `UNIQUE`); el cliente debe ser acudiente
del estudiante y tener dirección; la resolución debe estar **vigente** y el consecutivo dentro de su **rango
autorizado**; el consecutivo se bloquea con `SELECT ... FOR UPDATE`. Si la factura no se puede emitir al registrar
un pago (por ejemplo, la resolución venció), el pago se conserva y se factura después desde este submenú.

**Contenido de la factura** (`facturas/factura_NNNNNN.txt` y `.pdf`, con la ruta en pantalla): emisor (razón social,
NIT, dirección, teléfono y correo), número, fecha y hora de emisión, forma y medio de pago, adquiriente (acudiente:
nombre, documento, dirección, teléfono, correo), estudiante y grado, una línea por pago (descripción, recibo,
cantidad, valor unitario sin IVA, IVA y subtotal), subtotal, **IVA por tarifa** (el valor pagado ya incluye el IVA y
se separa al centavo) y total. Elementos **simulados**: CUFE (SHA-384 de número, fecha, hora, subtotal, IVA, otros
impuestos, total, NIT, documento del cliente, clave técnica y ambiente 2 = pruebas), resolución de habilitación y
código QR real (ZXing) con los mismos campos de un QR DIAN; en el TXT el QR se dibuja con caracteres de bloque.

### 6. Gestión de Recursos

Inventario con código único, categoría, estado y ubicación; préstamo a **estudiante o profesor** (solo si el recurso
está `Disponible`); devolución con fecha real que deja el recurso `Disponible`; los préstamos con fecha prevista
pasada se marcan `Vencido` automáticamente; historial por persona y por recurso.

### 7. Reportes y Estadísticas

Resumen general; ranking de promedios; promedio del curso por materia; historial académico; alertas de inasistencia;
ocupación de clubes; participación y podios en eventos; participación por estudiante; mora mayor a un mes; préstamos
vencidos; y **exportación a CSV** en `reportes/` (se abren directamente en Excel).

---

## 11. Manejo de Errores y Logs

### Sistema de logging

- `ErrorLogger` escribe en **`error_log.txt`**, en la raíz del proyecto (configurable con `log.file` en
  `config/app.properties`). El archivo está en `.gitignore`.
- Cada entrada tiene fecha y hora, nivel (`ERROR` / `WARN`), contexto (módulo y operación), mensaje y traza completa.
- **Política:** se registran los errores técnicos (SQL, conexión, escritura de la factura TXT/PDF o de los CSV, errores
  inesperados). Las validaciones de negocio no se registran porque son esperadas: se muestran al usuario.

### Estrategia de manejo de excepciones

| Excepción | Cuándo | Qué ve el usuario |
|---|---|---|
| `BusinessException` | Regla de negocio o validación (nota fuera de rango, cupo lleno, recurso no disponible...) | El motivo, y se vuelve a preguntar o regresa al menú |
| `DataAccessException` | Envuelve `SQLException` y traduce los códigos de MySQL (duplicado, llave foránea, `CHECK`, sin conexión, acceso denegado) | Un mensaje claro en español; el detalle técnico queda en `error_log.txt` |
| `OperationCancelledException` | El usuario escribe `0` en una selección | "Operación cancelada" |
| `MainMenuRequestedException` | El usuario escribe `inicio` | Regresa al menú principal |

- `BaseController` captura todas las excepciones en un solo lugar: ninguna operación cierra la aplicación.
- Todas las consultas usan `PreparedStatement` y `try-with-resources`. Las operaciones compuestas (registrar estudiante
  con acudiente, pago con factura, préstamo y devolución, inscripción a clubes, cobros masivos) se ejecutan en una
  transacción con `TransactionManager` (commit o rollback).

---

## 12. Capturas de Pantalla

Salidas reales de la aplicación (datos de prueba ficticios).

**Menú principal**

```text
Conectado a MySQL 8.0.46 (jdbc:mysql://localhost:3306/erp_little_horizons)

╔══════════════════════════════════╗
║   ERP Colegio Little Horizons    ║
║   Sistema de Gestión Educativa   ║
╚══════════════════════════════════╝

1. Gestión de Estudiantes
2. Gestión Académica
3. Gestión de Profesores
4. Actividades Extracurriculares
5. Gestión Financiera
6. Gestión de Recursos
7. Reportes y Estadísticas
8. Salir

Seleccione una opción:
```

**Submenú de un módulo**

```text
==============================================================================
  GESTIÓN DE ESTUDIANTES
==============================================================================
   1. Listar estudiantes
   2. Buscar estudiante por nombre o documento
   3. Ver ficha completa del estudiante
   4. Registrar estudiante (con acudiente)
   ...
  12. Contactos de emergencia
   0. Volver
  (Escriba 'inicio' en cualquier momento para ir al menú principal)
Seleccione una opción:
```

**Validación de una nota fuera de rango**

```text
Nota (0.0 a 5.0, un decimal): 6.1
[ERROR] La nota debe estar entre 0.0 y 5.0.
Nota (0.0 a 5.0, un decimal): 4.0
[OK] Nota 4.0 guardada para Santiago Pérez Cárdenas en Matemáticas (III 2026).
```

**Reporte de mora (deudas con más de un mes de atraso; columnas y filas resumidas)**

```text
+-------+--------------------------+---------+---------------------------------+------------+------------+-----------+-----------+
| Cobro | Estudiante               | Grado   | Acudiente                       | Teléfono   | Vence      | Días mora | Saldo     |
+-------+--------------------------+---------+---------------------------------+------------+------------+-----------+-----------+
| 102   | Emilio Quintero Montoya  | Sexto   | Sara Fernanda Montoya Bermúdez  | 3125221734 | 10/09/2026 | 68        | $ 350.000 |
| 44    | Santiago Pérez Cárdenas  | Segundo | María Lucía Cárdenas Sánchez    | 3234574913 | 10/09/2026 | 68        | $ 350.000 |
| 93    | Sara Lucía Torres Muñoz  | Quinto  | Emilio Camilo Torres Valencia   | 3205055529 | 20/09/2026 | 58        | $ 90.000  |
| ...   |                          |         |                                 |            |            |           |           |
+-------+--------------------------+---------+---------------------------------+------------+------------+-----------+-----------+
Total: 7 registro(s)
Total en mora: $ 2.015.000
```

**Pago confirmado: emite su factura automáticamente**

```text
--- Confirmación -------------------------------------------------------------
  Estudiante:                Santiago Pérez Cárdenas
  Monto:                     $ 350.000
  Método:                    Tarjeta
¿Confirmar el pago y emitir la factura? (s/n): s
[OK] Pago registrado. Recibo RC-000175 por $ 350.000 (Tarjeta).
[OK] Factura FE-000014 emitida a nombre de María Lucía Cárdenas Sánchez.
  Subtotal (sin IVA):        $ 350.000,00
  IVA:                       $ 0,00
  Total:                     $ 350.000,00
  CUFE (simulado):           5975fe32e2e30b8ad58bda5157e255664ba16560c1ab833c3acca3e43902df5ac36c0424656d9032e93d237e6f488601
[OK] Archivo TXT: ...\facturas\factura_000014.txt
[OK] Archivo PDF: ...\facturas\factura_000014.pdf
[!] La factura es una SIMULACIÓN ACADÉMICA: no está validada ni autorizada por la DIAN.
```

**Factura con varios pagos (Gestión Financiera > Facturas electrónicas > Emitir factura; QR omitido)**

```text
Pagos de Juliana Lucía Moreno Londoño que aún no tienen factura:
+----+-----------+------------+-----------------+-----------+---------------+----------------------+
| N° | Recibo    | Fecha      | Concepto        | Monto     | Método        | Pagó                 |
+----+-----------+------------+-----------------+-----------+---------------+----------------------+
| 1  | RC-000007 | 20/01/2026 | Matrícula       | $ 500.000 | Tarjeta       | Andrés Moreno Vargas |
| 2  | RC-000011 | 06/02/2026 | Uniforme        | $ 297.500 | Tarjeta       | Andrés Moreno Vargas |
| 3  | RC-000008 | 10/08/2026 | Pensión Mensual | $ 350.000 | Tarjeta       | Andrés Moreno Vargas |
| ...                                                                                                 |
+----+-----------+------------+-----------------+-----------+---------------+----------------------+
Números de los pagos a facturar (ej: 1,3) o 'todos': 1,99
[ERROR] El número 99 no está en la lista (1 a 5).
Números de los pagos a facturar (ej: 1,3) o 'todos': 1,2
Cliente sugerido: Andrés Moreno Vargas (CC 8069516505). ¿Facturar a su nombre? (s/n): s
¿Emitir la factura de 2 pago(s) por $ 797.500 a nombre de Andrés Moreno Vargas? (s/n): s
[OK] Factura FE-000015 emitida por $ 797.500 a nombre de Andrés Moreno Vargas.

==============================================================================
                 Colegio Internacional Little Horizons S.A.S.
                              NIT 901.234.567-8
                   Calle 140 # 19-45, Bogotá D.C., Colombia
          Tel. (601) 555 0140  |  facturacion@littlehorizons.edu.co
==============================================================================
                         FACTURA ELECTRÓNICA DE VENTA
                                No. FE-000015
               SIMULACIÓN ACADÉMICA - SIN VALIDEZ ANTE LA DIAN
==============================================================================
Fecha de emisión : 2026-11-17 18:53:42 (hora Colombia, UTC-05:00)
Forma de pago    : Contado (pagos ya recibidos)
Medio de pago    : Tarjeta
Moneda           : COP - Peso colombiano
------------------------------------------------------------------------------
ADQUIRIENTE (ACUDIENTE)
Nombre           : Andrés Moreno Vargas
Documento        : CC 8069516505
Dirección        : Diagonal 163 # 29-82, Niza, Bogotá D.C.
Teléfono         : 3149936949
Correo           : andres.moreno9@correo.com.co
Estudiante       : Juliana Lucía Moreno Londoño (RC 12161548173)
Grado            : Preescolar
------------------------------------------------------------------------------
DETALLE
Descripción                              Cant.  Valor unitario        Subtotal
------------------------------------------------------------------------------
Matrícula año 2026                           1    $ 500.000,00    $ 500.000,00
  Recibo RC-000007 | IVA 0% | Total línea $ 500.000,00
Uniforme escolar 2026                        1    $ 250.000,00    $ 250.000,00
  Recibo RC-000011 | IVA 19% | Total línea $ 297.500,00
------------------------------------------------------------------------------
                                      Subtotal (sin IVA):         $ 750.000,00
                                                  IVA 0%:               $ 0,00
                                                 IVA 19%:          $ 47.500,00
                                      TOTAL PAGADO (COP):         $ 797.500,00
==============================================================================
ELEMENTOS DE LA FACTURA ELECTRÓNICA (SIMULADOS)
CUFE (SHA-384):
  115f9cb8066c948c993421a7571f1dbc8b762c12530f3079
  7d94b7595f8b1fee01c10292a9a11b91728eb96b8f6d5e71
Ambiente         : 2 - Habilitación (pruebas)
Resolución de habilitación de facturación:
  Resolución DIAN No. 18760000000001 del 2026-01-15, prefijo FE, rango
  FE-000001 a FE-100000, vigente hasta 2028-01-15 (SIMULADA)
...
```

Los archivos de esa factura están en [`docs/ejemplos/factura_000015.txt`](docs/ejemplos/factura_000015.txt) (con el
código QR dibujado en texto) y [`docs/ejemplos/factura_000015.pdf`](docs/ejemplos/factura_000015.pdf).

**Exportación de reportes a CSV**

```text
Carpeta: ...\reportes
+-------------------------------------+-------+
| Archivo                             | Filas |
+-------------------------------------+-------+
| mora_20261117.csv                   | 7     |
| prestamos_vencidos_20261117.csv     | 6     |
| alertas_inasistencia_20261117.csv   | 4     |
| ranking_promedios_2026_20261117.csv | 37    |
| ocupacion_clubes_20261117.csv       | 30    |
+-------------------------------------+-------+
[OK] 5 reportes exportados (CSV separado por ';', se abren en Excel).
```

---

## 13. Problemas Conocidos y Limitaciones

- No hay autenticación ni roles de usuario: cualquier persona con acceso a la consola puede usar todos los módulos.
- La factura **no** es electrónica real: no se firma digitalmente ni se envía a la DIAN.
- Los datos de prueba están fechados alrededor del 15/11/2026; los reportes que dependen de la fecha actual (mora,
  préstamos vencidos, alertas de inasistencia) muestran resultados distintos según el día en que se ejecuten.
- La nota del período "Final" se registra manualmente; no se calcula a partir de los períodos I–IV.
- Las pruebas son *smoke tests* de integración en Java plano (sin JUnit) y necesitan la base de datos en ejecución.

---

## 14. Futuras Mejoras

- Usuarios con roles (administrador, profesor, tesorería) y autenticación.
- Cálculo automático de la nota Final con una política configurable (promedio simple o ponderado).
- Pruebas unitarias con JUnit y una base de datos de pruebas aislada.
- Pool de conexiones (por ejemplo HikariCP) para mejorar el rendimiento.
- Envío de la factura por correo al acudiente y reportes en PDF.

---

## 15. Licencia

Este proyecto se distribuye bajo la **Licencia MIT**. Consulte el archivo [`LICENSE`](LICENSE) para el texto completo.

Los datos de prueba (personas, documentos, teléfonos, correos y direcciones) son **ficticios** y se crearon solo para
demostrar el sistema.

---

## 16. Contacto

**Andrés Julián Rolón Ibero**

- Correo: yuliniri200712@gmail.com
- GitHub: [@yuliniri200712-bit](https://github.com/yuliniri200712-bit)
- Repositorio: <https://github.com/yuliniri200712-bit/erp-little-horizons>
