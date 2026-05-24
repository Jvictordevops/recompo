# PROGRESS

## DOING

### T-002 — Proyecto Android Studio + tema + navegación

**Estimación**: 4h (completado)

**Descripción**
Dependencias añadidas al versions catalog, estructura de paquetes creada, NavHost con
bottom navigation (5 tabs), pantallas stub para cada sección.

**Hecho cuando**
- [x] Build verde (`./gradlew assembleDebug`)
- [ ] Probado en móvil con bottom nav funcionando

---

### T-001 — Setup del repo y archivos de proceso (DONE)
**Estimación**: 5-7h

**Descripción**
Repo Git con primer push, 9 archivos de proceso con contenido real, 3 ADRs fundacionales,
CI verde, proyecto Android Studio mínimo (un Activity con Compose hola mundo).

**Hecho cuando**
- Repo Git con primer push y CI verde
- 9 archivos de proceso existen con contenido real
- 3 ADRs reflejan decisiones del plan
- CONVENTIONS.md cubre convenciones de código
- PROGRESS.md tiene T-002 a T-006 redactadas
- Proyecto compila y arranca con Compose hola mundo
- Probado en el móvil

---

## TODO

### T-002 — Proyecto Android Studio + tema + navegación
**Estimación**: 4h

**Descripción**
Crear proyecto Android en Android Studio con Kotlin + Jetpack Compose. Configurar
`build.gradle.kts` con versions catalog, tema Material 3, NavHost con rutas vacías para
las 5 secciones (Home, Nutrición, Entrenamiento, Actividad, Mediciones, Settings).
Estructura de paquetes según plan §1.1.

**Hecho cuando**
- Build verde (`./gradlew assembleDebug`)
- App arranca en móvil con bottom nav y pantallas stub
- Estructura de paquetes creada

---

### T-003 — Room: 8 entidades + DAOs + type converters + tests
**Estimación**: 5h

**Descripción**
Implementar todas las entidades Room del plan §2: `ComidaBase`, `Ejercicio`, `Sesion`,
`EjercicioEnSesion`, `Serie`, `EntradaComida`, `Actividad`, `Medicion`, `Conversacion`,
`MensajeIA`. Type converters para `LocalDate`, `Instant`, enums. DAOs básicos para cada
entidad. Tests con Room in-memory para DAOs críticos.

**Hecho cuando**
- Build verde
- Tests de DAOs críticos en verde (Medicion, EntradaComida, Sesion)
- `fallbackToDestructiveMigration()` configurado

---

### T-004 — DataStore UserSettings + wizard 3 pantallas
**Estimación**: 6h

**Descripción**
`UserSettings` completo en DataStore Preferences (§2.1). Wizard de primer arranque:
pantalla 1 (datos personales), pantalla 2 (plan + macros), pantalla 3 (backup SAF,
skippable). Lógica de "primer arranque" en `App.kt` o `MainActivity`.

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

_(vacío por ahora)_
