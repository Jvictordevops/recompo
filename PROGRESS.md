# PROGRESS

## DOING

_(ninguna)_

---

## TODO

### T-008 — Actividad + refactor a kcalObjetivo dinámico ✓ (pendiente prueba en móvil)
**Estimación**: 5-6h

**Descripción**
Dos fases:

**Fase A — Refactor del modelo kcalObjetivo (plan §2.1 v1.6, ADR-0004)**
Sustituir los 3 campos `kcalDescanso/Musculacion/Bici` de `UserSettings` por un único
`kcalBaseDia: Int` (déficit de mantenimiento). El `kcalObjetivo` del día se calcula
en runtime como `kcalBaseDia + sum(kcalQuemadas de actividades de hoy)`.
- Modificar `UserSettings` (data class) + `UserSettingsStore` (DataStore Preferences).
- Modificar paso 2 del wizard: un solo input de kcal base (default sugerido: 1900).
- Crear `domain/usecase/CalcularKcalObjetivoUseCase.kt` con su test unitario.
- Modificar `HomeViewModel`: combinar flow de actividades hoy + use case.
- `TipoDia` se mantiene como etiqueta visual informativa pero **no afecta al cálculo**.
- Como aún no hay datos reales en producción: borrar app + reconfigurar wizard vale
  como "migración" (no se implementa lectura del schema viejo).

**Fase B — Actividad: log manual (§2.5)**
Pantalla Actividad con lista + alta manual. Entidad `Actividad`: fecha, tipo (texto
libre: "bici", "caminata", "otro"), descripción opcional, duracionMin opcional,
kcalQuemadas (input manual obligatorio). CRUD básico. Sin integraciones Health Connect
ni Strava (decisión §8.3).

**Hecho cuando**
- Build verde + tests verdes (incluido test del use case)
- Wizard funciona con campo único `kcalBaseDia`
- Home muestra `kcalObjetivo = kcalBaseDia + Σ kcalQuemadas hoy` en tiempo real
- CRUD de actividad funcional; añadir/borrar recalcula objetivo en Home
- Probado en móvil

---

### T-009 — Entreno: versión simple
**Estimación**: 10h

**Descripción**
Pantalla de entreno en su versión MVP (§4.2 "Sesión de entreno simple"):
1. Iniciar sesión A/B/C desde Home si toca hoy.
2. Pre-sesión: lista de ejercicios con objetivos (series x reps, carga objetivo).
3. En curso: lista plana de ejercicios con inputs por serie (reps, carga, RIR 0-5).
   Persistencia simple al cerrar cada serie. Sin timer, sin gestión sofisticada
   de estado en rotación (intento simple de restore; si se pierde algo se reintroduce).
4. Post-sesión: resumen + nota libre + RIR global + botón "Generar próxima sesión"
   **visible pero deshabilitado** con tooltip "Disponible en Fase 2". La generación
   real con IA es trabajo de Fase 2.

Sin KEEP_SCREEN_ON, sin timer descanso, sin animaciones (Fase 3).

**Hecho cuando**
- Build verde + tests verdes
- Flujo completo: iniciar → registrar series → cerrar sesión
- Estado se persiste en BD por serie (no se pierde si cierras la pantalla)
- Botón "Generar próxima sesión" presente pero deshabilitado con tooltip
- Probado en móvil con una sesión real

---

### T-011 — Settings básico
**Estimación**: 4h

**Descripción**
Pantalla Settings con:
- Editar perfil (datos personales, plan).
- Editar macros objetivo: sólo `kcalBaseDia` y `proteinaObjetivoG` (tras refactor
  T-008 / ADR-0004).
- Reconfigurar carpeta de backup (SAF) si quieres cambiarla.
- Botones de export (capa 2 y 3 — los implementa T-010, aquí solo se exponen).
- Placeholder "Uso de IA" (la pantalla real es de Fase 2).

Cuando cambies de fase, editas `kcalBaseDia` aquí. No hay entidad Fase histórica en MVP.

**Hecho cuando**
- Build verde + tests verdes
- Edición de UserSettings funcional (cambios se reflejan en Home)
- Reconfigurar carpeta backup funcional
- Probado en móvil

---

### T-010 — Backup: JSON continuo + export XLSX
**Estimación**: 8-12h

**Descripción**
Tres capas de backup (§6.4):
- **Capa 1**: tras cambios significativos (cierre de sesión, fin del día, abandono de
  pantalla con cambios pendientes), escribir `recomposicion.json` en carpeta Drive
  elegida vía SAF. Trigger vía `BackupScheduler` con debounce global (no se invoca
  desde cada repositorio). Actualizar UserSettings: ultimoBackupOk,
  ultimoBackupError, ultimoBackupBytes. El chip de Home (ya implementado en T-005
  con estados visuales, pero hoy muestra default porque nada actualiza esos campos
  hasta esta tarea) pasará a reflejar el estado real.
- **Capa 2**: botón "Exportar a Excel" en Settings → genera
  `recomposicion_YYYY-MM-DD.xlsx` con fastexcel. Hojas: Resumen, Mediciones,
  Entrenamiento, Nutrición, Actividad (§6.5).
- **Capa 3**: botón "Exportar todo (ZIP)" → JSON + SQLite cruda + XLSX en un zip.

Sin WorkManager automático (decisión §6.4). Sin sincronización entre dispositivos.

Estimación amplia (8-12h) porque SAF + Drive es la zona más impredecible: permisos
que caducan silenciosamente, locking, validar que el JSON se re-importa sin perder
datos.

**Hecho cuando**
- Build verde + tests verdes
- JSON continuo escribe tras cambios significativos y actualiza tracking en UserSettings
- BackupChip de Home refleja correctamente OK / atrasado / falló
- Export XLSX funciona y abre limpio en Excel/LibreOffice
- Export ZIP contiene los 3 ficheros
- Probado en móvil con cuenta Drive real

---

### T-012 — Seed inicial de ComidaBase
**Estimación**: 1-2h

**Descripción**
Resuelve DT-002. Sin esto, el modo "Plantilla" de la pantalla de nutrición está vacío
y la app no es usable en serio. Seed hardcoded en código (no pantalla CRUD — eso es
Fase 3).

Disparador: en primer arranque (o cuando la tabla `ComidaBase` esté vacía), insertar
los registros iniciales tras completar el wizard.

**Datos a sembrar** (slots y macros a confirmar con Vic antes de codificar):
- 1 desayuno fijo: 250 ml leche semi + 40 g avena + 30 g HSN whey
- 3 tostadas: atún · lomo embuchado · pavo con queso
- 3 natillas: sola · + plátano · + plátano + uvas

Los macros (kcal/prot/grasa/carbo) de cada variante se rellenan con valores reales
calculados con Vic antes de codificar (no estimaciones de IA).

**Hecho cuando**
- Build verde + tests verdes
- Primer arranque con tabla vacía inserta las plantillas
- Selector de plantillas en nutrición muestra las variantes esperadas por slot
- Probado en móvil

---

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
**Afecta**: wizard (y cualquier formulario futuro con fechas)
**Problema**: el campo de fecha usa formato ISO (AAAA-MM-DD) con teclado numérico. Debería ser DD/MM/AAAA con `DatePickerDialog` de Material 3.
**Cuando atacar**: cuando se detecte otro formulario con fechas o antes de entregar el MVP.

### DT-002 — Seed de plantillas de comida
**Afecta**: pantalla Nutrición → modo Plantilla
**Problema**: `ComidaBase` está vacía, el selector de plantillas no muestra nada. El modo Plantilla es inútil hasta que haya datos.
**Cuando atacar**: antes de usar la app en serio, o cuando se implemente la pantalla de gestión de plantillas en Settings (Fase 3). Solución mínima: seed manual con `INSERT` o pantalla básica de CRUD en Settings.

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
