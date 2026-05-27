# ADR-0005 — Listado de sesiones de entreno: hoy + anteriores, editables, registro retroactivo

**Estado**: Aceptado
**Fecha**: 2026-05-26

## Contexto

La primera versión de T-009 (Entreno simple, §4.2 del plan) implementó una pantalla
en tres fases (PRE_SESION → EN_CURSO → POST_SESION) con un listado superior que
mostraba todas las sesiones existentes ordenadas por inserción.

Tras probarla en móvil con una sesión real (2026-05-26), Vic reportó seis problemas
de UX que afectan al flujo central de "registrar el entreno del día":

1. **RIR slider corta en 4** — el `Row` de botones 0..5 desborda en pantallas
   estándar y el 5 queda fuera.
2. **No se pueden borrar sesiones** — si creas una sesión por error queda en el
   listado para siempre.
3. **No hay vuelta atrás desde el resumen** — al llegar a POST_SESION no se puede
   volver a EN_CURSO si te has dejado una serie sin registrar.
4. **Sesiones de días distintos sin diferenciar** — una sesión del 24 aparece
   visualmente igual que la de hoy, sin sección ni jerarquía.
5. **RIR por serie es excesivo** — para una sesión de 8-10 ejercicios × 3 series
   son ~24 inputs de RIR. En la práctica el RIR es bastante estable por ejercicio.
6. **No se pueden registrar sesiones pasadas** — el "Nueva sesión" asume hoy y no
   permite elegir fecha, así que si te olvidas un entreno no puedes recuperarlo.

Además, Vic preguntó explícitamente cómo se organizaría el listado:

> "dónde irán las sesiones pasadas. serán editables. podré poner nuevas sesiones
> pasadas?"

Esta decisión queda fuera del alcance literal del plan v1.6 §4.2 (que describe
solo el flujo "registrar una sesión"), por eso este ADR cierra los huecos.

## Decisión

### 1. Listado: dos secciones en un solo scroll

`PantallaLista` muestra todas las sesiones en un `LazyColumn` ordenado por
`fechaPrevista` DESC con dos cabeceras lógicas:

```
HOY · martes 26 may
  [Card sesión de hoy — destacada, colorContainer primario]

ANTERIORES
  [Card 25 may · COMPLETADA · Sesión B]
  [Card 22 may · COMPLETADA · Sesión A]
  ...
```

- **"HOY"** sólo aparece si existe una sesión con `fechaPrevista == LocalDate.now()`.
- **"ANTERIORES"** lista el resto. No hay paginación en MVP (Vic entrena ~3 veces
  por semana → ~150 sesiones/año, scroll razonable durante años).
- Si no hay sesión hoy, se ve un CTA "Sin sesión para hoy — pulsa + para crear una".
- Cabeceras como `stickyHeader` para que se queden visibles al hacer scroll.

### 2. Todas las sesiones son editables

Tap en cualquier card → entra en la fase que corresponde según `estado`:

| `EstadoSesion` | Fase a la que entra |
|----------------|---------------------|
| `PLANIFICADA`  | PRE_SESION (puedes ajustar ejercicios y empezar) |
| `EN_CURSO`     | EN_CURSO (sigues registrando series) |
| `COMPLETADA`   | POST_SESION en modo edición (puedes editar notas, RIR global, y desde ahí volver a EN_CURSO para tocar series) |
| `OMITIDA`      | PRE_SESION (decisión: tratar como replanificable) |

No hay distinción entre "ver" y "editar". Es app personal de un solo usuario, no
hace falta modo lectura.

### 3. Registro retroactivo permitido

El dialog "Nueva sesión" pasa de capturar sólo `TipoSesion` a capturar:
- `TipoSesion` (A/B/C)
- `fechaPrevista` (DatePicker Material 3, default `LocalDate.now()`, sin restricción
  de rango — se puede elegir cualquier día pasado o futuro)

Caso de uso: registrar el entreno de ayer que no se metió, o planificar el de mañana.

**Restricción**: no se permiten dos sesiones del mismo `tipo` en el mismo
`fechaPrevista` (se valida en `crearSesion()` consultando el DAO antes de insertar).
Si ya existe, el dialog muestra error y propone abrir la existente.

### 4. Borrado de sesiones

Cada card del listado expone un icono de papelera (o swipe-to-delete) con
`AlertDialog` de confirmación. Al confirmar, cascada:

```
serieDao.deleteByEjercicioEnSesion(...)
ejercicioEnSesionDao.deleteBySesion(...)
sesionDao.delete(sesion)
```

Sin papelera ni undo en MVP. El JSON de backup (T-010) tendrá histórico si hace falta
recuperar algo, pero el caso típico es borrar sesión recién creada por error.

### 5. RIR pasa de "por serie" a "por ejercicio"

`Serie.rir` se mantiene en el schema (no se borra para no romper el plan §3.2 ni
descartar uso futuro), pero **deja de pedirse en el diálogo de registrar serie**.
En su lugar:

- `EjercicioEnSesion` recibe un campo nuevo `rir: Int?` (nullable).
- Cuando se registra la **última** serie de un ejercicio (el contador llega a
  `seriesObjetivo`), el diálogo de serie se transforma en "Serie N · RIR del
  ejercicio" y captura `reps`, `carga`, y el RIR global del ejercicio (0-5).
- Las series internas se guardan con `rir = -1` como sentinel "no aplica"
  (o se duplica el RIR del ejercicio en cada serie — decisión de implementación,
  el ADR sólo fija la **UX**: un input de RIR por ejercicio, no 3-4).
- En POST_SESION el resumen muestra `RIR ${ejercicio.rir}` una vez por ejercicio.

Esto reduce los inputs de RIR de ~24 a ~8-10 por sesión sin perder la
información que el usuario realmente registra mentalmente.

### 6. Vuelta atrás desde POST_SESION

`PantallaPostSesion` añade un `TopAppBar` con `IconButton(ArrowBack)` que
ejecuta `viewModel.volverAEnCurso()`. Este método cambia `fase = EN_CURSO` sin
tocar nada del estado de la sesión. El botón "Cerrar sesión" sigue siendo el
camino feliz; la flecha atrás es para el caso "uy, me dejé una serie".

### 7. RIR slider — fix visual

El `Row` actual con 6 botones 0..5 desborda. Solución: `Row` con
`Arrangement.SpaceBetween` y botones cuadrados (`Modifier.weight(1f)`) en lugar de
`spacedBy(4.dp)` con padding fijo. Cada botón ocupa 1/6 del ancho.

## Alternativas descartadas

- **Tabs "Hoy" / "Histórico"**: añade clic extra para ver cualquier cosa, y en una
  app personal de uso diario el histórico también es información útil al abrir.
- **Mover ejecución de POST_SESION a un step de "confirmar guardado"** (sin
  permitir volver): peor UX para errores comunes, no aporta nada.
- **Borrar `Serie.rir` del schema**: rompería tests existentes y obligaría a una
  migración destructiva sin motivo claro — Fase 2 (IA) podría querer RIR por serie.
  Se mantiene el campo, se cambia sólo la captura.
- **DatePicker con restricción "sólo pasado"**: bloquea planificar a futuro, que
  es un caso de uso válido (planificar mañana A o B según cómo te encuentres).

## Consecuencias

- **Cambio de schema**: `EjercicioEnSesion` gana columna `rir: Int?`. Schema sube
  a v2. Como sigue activo `fallbackToDestructiveMigration(true)` (CONVENTIONS.md
  + plan §3.1), no requiere migración real — basta con borrar la app o
  desinstalar. Vic no tiene datos productivos todavía.
- **`SesionDao` gana queries**:
  - `getByFecha(fecha: LocalDate, tipo: TipoSesion): Sesion?` (validación de
    duplicado en `crearSesion`).
  - `delete(sesion: Sesion)` (borrado en cascada manual desde ViewModel — Room no
    hace cascadas si no las definimos en `@ForeignKey`).
- **`EntrenoViewModel` gana acciones**:
  - `eliminarSesion(sesionId: Long)` con confirmación previa en UI.
  - `volverAEnCurso()` desde POST_SESION.
  - `crearSesion()` valida duplicado por (fecha, tipo) antes de insertar.
- **`FormSerie` cambia**: en la última serie de un ejercicio captura RIR del
  ejercicio; en las anteriores no muestra el control de RIR.
- **`Sesion.fechaEjecutada`** queda intacto (se sigue marcando como `Instant.now()`
  al pulsar "Empezar" — independiente de `fechaPrevista`).
- **Búsqueda/scroll**: con 150+ sesiones al año, el scroll seguirá siendo
  razonable durante 2-3 años. Si pasa a ser problema → buscador en Settings o
  filtros por tipo/mes (no MVP).
- **Documentación del flujo**: este ADR completa lo que el plan §4.2 deja abierto
  sobre el listado, sin renumerar ni reescribir el plan (CLAUDE.md regla 6).

## Tareas afectadas

- **T-009** (Entreno: versión simple): iteración 2. Implementa los puntos 1-7
  de la decisión sobre el código de la iteración 1 ya mergeada.
- **T-010** (Backup JSON + XLSX): el export debe contemplar el campo nuevo
  `EjercicioEnSesion.rir` en la hoja "Entrenamiento".
- **DT-001** (DatePicker español DD/MM/AAAA): se ataca de paso en este mismo
  ticket porque el dialog "Nueva sesión" necesita DatePicker — se aplica el
  mismo componente reutilizable a wizard si queda tiempo, o se documenta el
  reemplazo pendiente.
