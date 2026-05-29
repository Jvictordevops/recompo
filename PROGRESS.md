# PROGRESS

## DOING

_(vacío)_

---

## DONE (reciente)

### T-012 — Seed inicial de ComidaBase

**Commit**: hecho

**Estado**: build verde, 43 tests verdes, probado en móvil ✓ (2026-05-29).

- `data/db/dao/ComidaBaseDao.kt` — añadido `suspend fun count(): Int`.
- `App.kt` — `seedComidasBaseIfEmpty()`: si `ComidaBase` está vacía, inserta 7 plantillas (1 desayuno, 3 tostadas, 3 natillas). Macros calculados con cantidades reales de Vic.
- `ui/nutricion/NutricionScreen.kt` — dropdown de plantillas muestra todas las variantes activas independientemente del slot (antes filtraba por slot del dialog, lo que impedía usar una tostada en merienda).

---

### T-010 — Backup: JSON continuo + export XLSX

**Commit**: hecho (confirmado por Vic)

**Estado**: build verde, 43 tests verdes, probado en móvil ✓ (2026-05-29).

- `data/backup/BackupDto.kt` — DTOs `@Serializable` para todas las tablas (11 clases). Fechas/enums como String para portabilidad.
- `data/backup/BackupSerializer.kt` — lee todas las tablas Room (`.first()` sobre Flows + `suspend getAll()` en los que faltaba), construye `BackupData`, serializa a JSON y escribe en carpeta SAF. Actualiza `ultimoBackupOk/Error/Bytes` en UserSettings tras cada backup. Helper `escribirEnCarpeta()` reutilizado por exportadores.
- `data/backup/XlsxExporter.kt` — genera `.xlsx` sin dependencias externas (ZIP de XML OpenXML directo). 5 hojas: Resumen, Mediciones, Entrenamiento (una fila por ejercicio), Nutrición, Actividad.
- `data/backup/ZipExporter.kt` — genera ZIP con `recomposicion.json` + `recomposicion_YYYY-MM-DD.xlsx` + `recomposicion_raw.db` (con PRAGMA wal_checkpoint antes de copiar).
- `data/db/dao/EntradaComidaDao.kt` — añadido `suspend fun getAll()`.
- `data/db/dao/SerieDao.kt` — añadido `suspend fun getAll()`.
- `data/db/dao/EjercicioEnSesionDao.kt` — añadido `suspend fun getAll()`.
- `data/db/dao/MensajeIADao.kt` — añadido `suspend fun getAll()`.
- `ui/settings/SettingsViewModel.kt` — añadidos `hacerBackup()`, `exportarXlsx()`, `exportarZip()`, `descartarMensaje()`. Estado `exportando: Boolean` + `mensajeResultado: String?`. Constructor recibe `RecompoDatabase`.
- `ui/settings/SettingsScreen.kt` — `CardBackup` con botón "Hacer backup ahora" (visible si carpeta configurada). `CardExport` con botones "Exportar a Excel" y "Exportar todo (ZIP)" activos (deshabilitados si no hay carpeta). Dialog de resultado.
- `MainActivity.kt` — `SettingsViewModelFactory` recibe `app.database`.
- `gradle/libs.versions.toml` + `app/build.gradle.kts` — añadido `androidx.documentfile:documentfile:1.0.1`.
- Build verde, tests verdes ✓

---

### T-011 — Settings básico

**Commit**: `7048013 feat(settings): T-011 pantalla ajustes con edición de perfil, macros y backup`

**Estado**: build verde, 43 tests verdes (10 nuevos), probado en móvil
(2026-05-28).

- `domain/validation/UserSettingsValidation.kt` — funciones puras
  (`parseNombre`, `parseFase`, `parseAltura`, `parsePesoKg`, `parseKcal`,
  `parseProteinaG`, `parseLocalDate`) con `Result<T>` y mensajes de error
  como constantes. Tests AAA sin Robolectric (10 casos).
- `ui/common/DialogDatePicker.kt` — composable común extraído de
  `EntrenoScreen.kt:367-388`. Misma firma `(fechaInicial, onFechaSeleccionada,
  onDismiss)`. EntrenoScreen pasa a importarlo (cero cambio funcional).
- `ui/settings/SettingsViewModel.kt` + Factory — sealed `SettingsDialog`
  (Texto/Entero/Decimal/Fecha/EditarSexo/ConfirmarQuitarBackup) con error
  por dialog. `StateFlow<SettingsUiState>` combina `store.settings + _dialog`.
  `guardar()` valida via `UserSettingsValidation` y persiste; SAF via
  `cambiarCarpetaBackup(uri)` con `takePersistableUriPermission`.
- `ui/settings/SettingsScreen.kt` — reescrito. 6 cards (Perfil/Plan/
  Macros/Backup/Export/IA) con `CardSeccion` + `CampoItem` (ListItem
  clickable). `DialogHost` rutea por tipo a `DialogTextoEditor`
  (parametrizado por `KeyboardType`), `DialogDatePicker`, `DialogSexo`
  con RadioButtons o `AlertDialog` de confirmación. Botones de export
  deshabilitados con etiqueta "Disponible en T-010". Card IA con
  placeholder "Disponible en Fase 2 (Claude API)".
- `ui/wizard/WizardViewModel.kt` — `validateStep1/2` refactorizado a
  llamadas a `UserSettingsValidation`. API pública intacta.
- `data/UserSettingsStore.kt` — añadido `clearBackupUri()`; `save()`
  ahora usa helper `setOrRemove()` para eliminar la pref cuando el valor
  es null (bugfix: antes los nullables nunca se podían limpiar).
- `MainActivity.kt` — `SettingsViewModel` cableado con `application` +
  `userSettingsStore` y pasado a `MainAppContent`.
- Build verde, tests verdes (43), probado en móvil ✓

---

### T-009 — Entreno: versión simple + iteración UX

**Commit**: pendiente (este commit)

**Estado**: iteración 1 + iteración 2 completadas y probadas en móvil
(2026-05-27).

**Iteración 1**
- `domain/model/Enums.kt` — `GrupoMuscular` y `PatronMovimiento` ampliados
  para cubrir el catálogo real (HOMBRO_LATERAL, ISQUIOS, GEMELOS, BISAGRA,
  AISLAMIENTO, EXPLOSIVO, etc.).
- `data/db/dao/EjercicioDao.kt` — métodos `count()`, `insertAll()`,
  `getByNombre()` para soportar el seed idempotente.
- `app/src/main/assets/seed/ejercicios.json` — catálogo de 25 ejercicios
  desde el plan v2 de Vic (nombre, grupo, patrón, equipamiento, notas).
- `App.kt` — `CoroutineScope` en `onCreate()` ejecuta `seedEjerciciosIfEmpty()`
  (idempotente: sólo siembra si la tabla está vacía).
- `app/src/main/assets/seed/sesiones_template.json` — plantillas de
  sesiones A (10 ej), B (8 ej) y C (8 ej) con cargas objetivo y notas de
  progresión exportadas del Excel actual de Vic.
- `ui/entreno/EntrenoViewModel.kt` — máquina de estados (LISTA, PRE_SESION,
  EN_CURSO, POST_SESION), `crearSesion()` llama a `poblarDesdeTemplate()`
  que lee el JSON e inserta los `EjercicioEnSesion` automáticamente.
- `ui/entreno/EntrenoScreen.kt` — cuatro composables, dialogs de añadir
  ejercicio, registrar serie (con RIR 0-5) y post-sesión con botón
  "Generar próxima sesión (Fase 2)" deshabilitado.
- `MainActivity.kt` — `EntrenoViewModel` cableado con `applicationContext`
  para acceso a assets.
- Build verde, tests verdes, probado en móvil ✓

**Iteración 2 — UX tras probar en móvil (ver ADR-0005)**

Tras probar con una sesión real (2026-05-26), Vic identificó seis huecos
de UX. ADR-0005 documenta la arquitectura del listado de sesiones (sección
Hoy + Anteriores, todas editables, registro retroactivo permitido) y los
cambios siguientes:

1. **Listado con secciones** — `PantallaLista` reorganizada con
   `stickyHeader` "HOY" / "ANTERIORES", ordenada por `fechaPrevista` DESC.
   Tarjeta de hoy con `colorContainer` primario.
2. **Borrado de sesiones** — icono papelera por card con `AlertDialog` de
   confirmación; cascada manual desde ViewModel (`serieDao` →
   `ejercicioEnSesionDao` → `sesionDao.delete`).
3. **DatePicker en "Nueva sesión"** — `fechaPrevista` seleccionable
   (default hoy, sin restricción de rango). Validación: no permitir dos
   sesiones del mismo `(fecha, tipo)`. Solución a DT-001.
4. **RIR por ejercicio** — eliminado control RIR del diálogo de cada serie;
   se captura RIR sólo en la última serie del ejercicio. Nueva columna
   `EjercicioEnSesion.rir: Int?`. Schema sube a v2 (destructive migration
   sigue activa, sin datos productivos). El campo `Serie.rir` se mantiene
   en el schema para Fase 2.
5. **Vuelta atrás desde POST_SESION** — `TopAppBar` con `ArrowBack` que
   llama a `viewModel.volverAEnCurso()`.
6. **Fix RIR slider** — botones 0..5 con `weight(1f)` y
   `Arrangement.SpaceBetween` para que no se corten en pantalla estándar.

Crash al primer arranque tras el upgrade v1→v2: resuelto desinstalando
la app antes de instalar el APK nuevo (colisión schema con instalación
previa, `fallbackToDestructiveMigration(true)` no llegaba a actuar).

Cambios adicionales del ViewModel: `abrirPreSesion()` enruta según estado
(COMPLETADA→POST_SESION, EN_CURSO→EN_CURSO, PLANIFICADA→PRE_SESION) para
que las sesiones ya completadas sigan siendo editables.

**Build verde, tests verdes, probado en móvil ✓**

---

## TODO


### T-013 — Smoke test MVP completo
**Estimación**: 1-2h

**Descripción**
No es código nuevo. Validación end-to-end del MVP usando la app durante un día real,
para pillar bugs de integración que los tests unitarios no ven.

Checklist:
1. Limpiar datos → wizard primer arranque con datos reales (nombre, plan,
   kcalBaseDia, proteína, carpeta backup).
2. Registrar 3 comidas del día (al menos 1 plantilla + al menos 1 libre).
3. Iniciar y cerrar 1 sesión de entreno completa (con sus series + RIR).
4. Registrar 1 actividad (bici o caminata) con kcalQuemadas.
5. Tomar 1 medición con todos los campos crudos.
6. Verificar Home: totales kcal/proteína, tipoDia visible, `kcalObjetivo` refleja
   la actividad sumada (refactor T-008), chip de backup, tarjeta sesión.
7. Disparar backup manual o forzar trigger.
8. Verificar JSON en Drive (tamaño > 0, contiene todos los inserts).
9. Reiniciar app: todo persiste y Home recompone correctamente.

**Hecho cuando**
- Los 9 pasos del checklist completados sin bugs bloqueantes
- Bugs menores documentados como tareas nuevas o entradas en DEUDA TÉCNICA
- DT-001 (DatePicker) revisitada: ¿sigue abierta? ¿se ataca antes de cerrar MVP?

---

## DONE (reciente)

### T-008 — Actividad + refactor a kcalObjetivo dinámico
**Commit**: `225513d feat(actividad): T-008 actividad CRUD + refactor kcalObjetivo dinámico`
- `domain/model/UserSettings.kt` — sustituidos `kcalDescanso/kcalMusculacion/kcalBici` por `kcalBaseDia: Int`
- `data/UserSettingsStore.kt` — clave única `kcal_base_dia`
- `ui/wizard/WizardViewModel.kt` + `WizardScreen.kt` — paso 2 con un solo campo kcal base (default 1900)
- `domain/usecase/CalcularKcalObjetivoUseCase.kt` — `kcalBaseDia + Σ kcalQuemadas`; 5 tests unitarios
- `ui/home/HomeViewModel.kt` — usa el use case en lugar de selección por TipoDia
- `data/db/dao/ActividadDao.kt` — añadido `getAll()` ordenado por fecha DESC
- `ui/actividad/ActividadViewModel.kt` + `ActividadScreen.kt` — CRUD con LazyColumn + FAB + dialog
- `MainActivity.kt` — tab Actividad en bottom nav; nav sin etiquetas (6 tabs, solo iconos)
- Build verde, tests verdes ✓

---

### T-007 — Mediciones: CRUD + cálculos automáticos
**Commit**: `f080d45 feat(mediciones): T-007 pantalla mediciones con CRUD y cálculos automáticos`
- `domain/calc/MedicionCalcs.kt` — fórmula Navy Hodgdon-Beckett (cm), IMC, masa grasa/magra, WHR
- `domain/MedicionCalcsTest.kt` — 12 tests unitarios, todos en verde
- `ui/mediciones/MedicionesViewModel.kt` — StateFlow<MedicionesUiState> + MedicionFormState + CRUD; snapshot de alturaCmEnLaMedicion y faseTexto congelados al crear (no sobreescriben en edición)
- `ui/mediciones/MedicionesScreen.kt` — LazyColumn más reciente primero + FAB + Dialog scrollable con inputs crudos, grasaPctOverride checkbox, hito y notas
- `MainActivity.kt` — MedicionesViewModelFactory cableado con medicionDao + userSettingsStore
- Build verde, tests verdes ✓

---

### T-006 — Nutrición: log manual + selector de plantillas
**Estimación**: 6h

**Descripción**
Pantalla de nutrición: log de comidas del día por slot (DESAYUNO, ALMUERZO, COMIDA,
MERIENDA, CENA). Selector de variante desde `ComidaBase`. Entrada manual directa
(kcal/prot/grasa/carbo). Totales del día actualizados en tiempo real. Sin IA aún.
CRUD de `EntradaComida`.

**Commit**: `feat(nutricion): T-006 pantalla nutrición con log manual y selector de plantillas`
- `NutricionViewModel.kt` — StateFlow<NutricionUiState> + DialogState con CRUD de EntradaComida
- `NutricionScreen.kt` — LazyColumn con TarjetaTotalesDelDia + 5 slots; tap-to-edit + borrar por entrada
- `AnadirComidaDialog` — modo Plantilla (dropdown por slot desde ComidaBase) y modo Libre; validación de campos
- `MainActivity.kt` — NutricionViewModelFactory cableado con DAOs de App.database
- fix: Icons.Filled.ShowChart → AutoMirrored
- Build verde, tests verdes, probado en móvil ✓

---

## DEUDA TÉCNICA

### DT-001 — Fechas en formato español + DatePicker
**Estado**: ✅ resuelta en T-009 iter 2 (DatePicker de Material 3 con locale "es" introducido en "Nueva sesión"). El wizard sigue con input ISO, pero ya hay patrón reutilizable cuando se aborde Settings (T-011).

### DT-002 — Seed de plantillas de comida
**Afecta**: pantalla Nutrición → modo Plantilla
**Problema**: `ComidaBase` está vacía, el selector de plantillas no muestra nada. El modo Plantilla es inútil hasta que haya datos.
**Cuando atacar**: antes de usar la app en serio, o cuando se implemente la pantalla de gestión de plantillas en Settings (Fase 3). Solución mínima: seed manual con `INSERT` o pantalla básica de CRUD en Settings.

### DT-003 — Fecha en nombre del fichero JSON de backup
**Afecta**: `BackupSerializer.kt` → `hacerBackup()`
**Problema**: el backup continuo siempre escribe `recomposicion.json`, sobrescribiendo el anterior. No hay histórico de backups en la carpeta Drive.
**Cuando atacar**: bajo demanda, antes de migrar datos importantes. Solución: añadir fecha al nombre (`recomposicion_YYYY-MM-DD.json`) o mantener los últimos N ficheros.

---

## DONE

### T-005 — Pantalla Home
**Commit**: pendiente (prueba en móvil primero)
- `HomeViewModel.kt` — StateFlow<HomeUiState> combinando 4 flows (settings, entradas, sesiones, actividades)
- `TipoDia` enum derivado: MUSCULACION si hay sesión activa hoy, BICI si hay actividad "bici", DESCANSO si no
- `kcalObjetivo` seleccionado automáticamente según tipoDia desde UserSettings
- `BackupChip.kt` en `ui/common/` — estados: sin configurar / pendiente / OK (Xh) / atrasado (>48h ⚠) / falló ⚠
- `HomeScreen.kt` — LazyColumn: encabezado (nombre + fecha + chip tipo día), tarjeta macros (kcal + proteína con LinearProgressIndicator), tarjeta sesión (si toca), BackupChip
- `MainActivity.kt` — HomeViewModelFactory cableado con DAOs de App.database
- Build verde, tests verdes ✓

---

### T-001 — Setup del repo y archivos de proceso
**Commit**: primer push a `jvictordevops/recompo` rama `main`
- Repo Git + GitHub + git-personal script para push con cuenta personal
- 9 archivos de proceso: CLAUDE.md, PROGRESS.md, CONVENTIONS.md, README, 3 ADRs, local.properties.example
- CI: `.github/workflows/ci.yml`
- Proyecto Android Studio (Empty Activity, Kotlin + Compose, minSdk 26)
- APK debug instalado y probado en móvil ✓

### T-002 — Proyecto Android Studio + dependencias + navegación
**Commit**: `feat: T-002 dependencias, estructura paquetes y navegación con bottom nav`
- Versions catalog completo: Room, KSP 2.3.8, Navigation, DataStore, OkHttp, Retrofit, kotlinx.serialization, Timber, Coroutines, Robolectric, Turbine
- app/build.gradle.kts: Java 17, KSP, serialización, BuildConfig con CLAUDE_API_KEY
- Estructura de paquetes: data/, domain/, ui/{home,nutricion,entreno,mediciones,actividad,settings,common}
- assets/tools/ y assets/prompts/ creados
- Bottom navigation con 5 tabs: Home, Nutrición, Entreno, Mediciones, Ajustes
- Pantallas stub para cada sección
- APK instalado y bottom nav probado en móvil ✓

### T-004 — DataStore UserSettings + wizard 3 pantallas
**Commit**: `feat: T-004 UserSettings DataStore y wizard de primer arranque`
- `domain/model/UserSettings.kt` — data class completa (§2.1)
- `data/UserSettingsStore.kt` — DataStore Preferences con Flow<UserSettings?>, Flow<Boolean> setupDone, save(), markSetupDone()
- `App.kt` — Application con singletons lazy (database, userSettingsStore)
- `ui/wizard/WizardViewModel.kt` — WizardState + validación por paso + WizardViewModelFactory
- `ui/wizard/WizardScreen.kt` — 3 pasos: datos personales, plan+macros, backup SAF (skippable)
- `MainActivity.kt` — lógica primer arranque: null→splash, false→wizard, true→app
- `AndroidManifest.xml` — android:name=".App"
- Build verde, tests en verde ✓
- Probado en móvil: wizard aparece en primer arranque, no vuelve a aparecer ✓

### T-003 — Room: entidades + DAOs + type converters + tests
**Commit**: `feat: T-003 Room entities, DAOs, type converters y tests in-memory`
- 9 enums en `domain/model/Enums.kt`: SlotComida, GrupoMuscular, PatronMovimiento, TipoSesion, EstadoSesion, OrigenSesion, TipoConversacion, RolMensaje, Sexo
- Type converters Room para LocalDate, Instant y todos los enums en `data/db/Converters.kt`
- 10 entidades: ComidaBase, Ejercicio, Sesion, EjercicioEnSesion, Serie, EntradaComida, Actividad, Medicion, Conversacion, MensajeIA
- Índices en columnas FK/fecha frecuentes
- 10 DAOs con operaciones básicas + queries críticas (getTotalesDelDia, getPendientes, getLatest…)
- `RecompoDatabase` con `fallbackToDestructiveMigration(true)`
- 16 tests in-memory Robolectric, todos en verde ✓
