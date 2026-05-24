# PROGRESS

## DOING

_(ninguna)_

---

## DONE (reciente)

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
