# PROGRESS

## DOING

_(ninguna — siguiente tarea: T-004)_

---

## TODO

### T-004 — DataStore UserSettings + wizard 3 pantallas
**Estimación**: 6h

**Descripción**
`UserSettings` completo en DataStore Preferences (§2.1). Wizard de primer arranque:
pantalla 1 (datos personales), pantalla 2 (plan + macros), pantalla 3 (backup SAF,
skippable). Lógica de "primer arranque" en `MainActivity`.

**Hecho cuando**
- Wizard aparece en primer arranque y no vuelve a aparecer
- `UserSettings` persiste correctamente
- Probado en móvil (reinstalar app para probar primer arranque)

---

### T-005 — Pantalla Home
**Estimación**: 4h

**Descripción**
Dashboard del día: tipo de día (descanso/musculación/bici), resumen de macros consumidos
vs objetivo, sesión del día si toca, `BackupChip` (estado del último backup). Datos
calculados desde el log del día en Room.

**Hecho cuando**
- Home muestra resumen real del día (aunque esté vacío)
- BackupChip visible (aunque backup no esté configurado aún)
- Build y prueba en móvil

---

### T-006 — Nutrición: log manual + selector de plantillas
**Estimación**: 6h

**Descripción**
Pantalla de nutrición: log de comidas del día por slot (DESAYUNO, ALMUERZO, COMIDA,
MERIENDA, CENA). Selector de variante desde `ComidaBase`. Entrada manual directa
(kcal/prot/grasa/carbo). Totales del día actualizados en tiempo real. Sin IA aún.
CRUD de `EntradaComida`.

**Hecho cuando**
- Puedes registrar una comida desde plantilla y desde entrada libre
- Los totales del día se actualizan
- Home refleja los macros del día
- Probado en móvil

---

## DONE

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

### T-003 — Room: entidades + DAOs + type converters + tests
**Commit**: `feat: T-003 Room entities, DAOs, type converters y tests in-memory`
- 9 enums en `domain/model/Enums.kt`: SlotComida, GrupoMuscular, PatronMovimiento, TipoSesion, EstadoSesion, OrigenSesion, TipoConversacion, RolMensaje, Sexo
- Type converters Room para LocalDate, Instant y todos los enums en `data/db/Converters.kt`
- 10 entidades: ComidaBase, Ejercicio, Sesion, EjercicioEnSesion, Serie, EntradaComida, Actividad, Medicion, Conversacion, MensajeIA
- Índices en columnas FK/fecha frecuentes
- 10 DAOs con operaciones básicas + queries críticas (getTotalesDelDia, getPendientes, getLatest…)
- `RecompoDatabase` con `fallbackToDestructiveMigration(true)`
- 16 tests in-memory Robolectric, todos en verde ✓
