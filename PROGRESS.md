# PROGRESS

## DOING

_(vacío)_

---

## DONE (reciente)

### T-019 — Pantalla "Uso de IA" en Settings (2026-05-31)
**Estado**: build verde, tests verdes, probado en móvil ✓ (2026-05-31).

- `data/db/entity/UsoIA.kt` — nueva entidad: timestamp, funcion (FuncionIA), proveedor (ProveedorIA), modelo, tokensIn, tokensOut, costeUsd.
- `data/db/dao/UsoIADao.kt` — insert + getDesde(epochMilli): Flow.
- `domain/ai/TarifasIA.kt` — tabla centralizada de tarifas por modelo (Sonnet 4.6: $3/M in + $15/M out; modelos desconocidos: 0).
- `domain/model/Enums.kt` — añadidos FuncionIA (PARSEO_COMIDA / GENERACION_SESION / CHAT) y ProveedorIA (CLAUDE_NATIVO / OPENROUTER).
- `data/db/Converters.kt` — type converters para FuncionIA y ProveedorIA.
- `data/db/RecompoDatabase.kt` — versión 4→5; MIGRATION_4_5 crea tabla UsoIA con índices.
- `data/ai/ParseComidaUseCase.kt` — inserta en UsoIA tras cada llamada real a la API.
- `domain/usecase/GenerarSesionUseCase.kt` — inserta en UsoIA tras cada llamada real (fallback no cuenta).
- `ui/chat/ChatViewModel.kt` — inserta en UsoIA tras cada respuesta del asistente (además de MensajeIA para historial).
- `MainActivity.kt` — usoIADao cableado a los tres puntos de entrada.
- `ui/settings/SettingsViewModel.kt` — UsoIaStats con costeEurSemana + desglose por función; alerta si tokens suben >20% vs semana anterior.
- `ui/settings/SettingsScreen.kt` — CardIA con total + fila por función activa.
- `test/ParseComidaUseCaseTest.kt` — fake UsoIADao añadido.

---

### T-018 — Chat conversacional (2026-05-31)
**Estado**: build verde, tests verdes, probado en móvil ✓ (2026-05-31).

- `domain/usecase/ChatUseCase.kt` — wrapper Claude API para chat sin tool calling; `Resultado.Exito(texto, tokensIn, tokensOut)` / `Resultado.Fallo`.
- `ui/chat/ChatViewModel.kt` — carga o crea `Conversacion` al init; construye system prompt dinámico desde `system_base.txt` con marcadores de perfil + contexto del día (macros, sesión activa, últimas 4 mediciones); truncado a MAX_MENSAJES_CHAT=20 y MAX_PROMPT_CHARS=8000 eliminando los más antiguos; persiste cada turno en `MensajeIA` con tokensIn/Out.
- `ui/chat/ChatScreen.kt` — burbujas de chat (usuario derecha, asistente izquierda), spinner durante llamada, snackbar de error, scroll automático al último mensaje, TopAppBar con flecha volver, `imePadding()`.
- `data/db/dao/MedicionDao.kt` — añadido `getRecientes(limit: Int): List<Medicion>`.
- `ui/Screen.kt` — añadido `Screen.Chat`.
- `ui/home/HomeScreen.kt` — `TarjetaChat` siempre visible; parámetro `onNavigateToChat`.
- `MainActivity.kt` — `chatViewModel` cableado; composable `Screen.Chat`; callback `onNavigateToChat` en Home.

---

### Entreno: seedId, histórico seed y 6 bugs (2026-05-30)
**Commit**: `feat(entreno): seedId en entidades, seed histórico, recuperación y 6 bugs`

**Estado**: build verde, probado en móvil ✓ (2026-05-30).

- `data/db/entity/Sesion.kt` + `Ejercicio.kt` — añadidos `seedId: String?` y `activo: Boolean` (Sesion).
- `data/db/RecompoDatabase.kt` — versión 3→4; `MIGRATION_3_4` añade columnas `seedId`/`activo` en Sesion y `seedId` en Ejercicio.
- `data/db/dao/SesionDao.kt` — `getBySeedIdAny()` sin filtro `activo` para recovery; todos los SELECT existentes filtran `activo = 1`.
- `App.kt` — `seedEjerciciosGymFase2()`: inserta ejercicios por `seedId` (idempotente). `seedSesionesHistoricoIfNeeded()`: inserta 15 sesiones históricas con guard DataStore `sesionesHistoricoSeeded`. `recoverMissingSeedSesiones()`: restaura seeds PREPARADA perdidos — maneja soft-deleted (reactiva), OMITIDA (revierte a PREPARADA), sin seedId (reusa sesión existente del tipo).
- `assets/seed/sesiones_seed.json` + `ejercicios_gym_fase2.json` — añadidos campos `seedId`.
- **Bug 1** — peso corporal: `cargaKg` null permitido en `guardarSerie()`; muestra `"PC"` en pantalla (antes `"nullkg"`).
- **Bug 2** — cancelar EN_CURSO: `confirmarCancelarSesion()` revierte a PREPARADA + `fechaEjecutada = null` + borra series del intento (antes ponía OMITIDA).
- **Bug 3** — sesión vacía: `irAPostSesion()` bloquea con `DialogSesionVacia` si no hay ninguna serie; opciones: continuar igualmente o descartar.
- **Bug 4** — múltiples EN_CURSO: `iniciarSesion()` detecta conflicto y muestra `DialogEnCursoConflicto`; usuario elige reanudar la existente o cancelarla y arrancar la nueva.
- **Bug 5** — soft-delete: `confirmarEliminarSesion()` y `confirmarReemplazarPreparada()` usan `activo = false`; ningún camino hace `DELETE` físico de sesión.
- **Bug 6** — sesión manual: `crearSesionManual()` + `duplicarSesion()` reemplazan `poblarDesdeTemplate()` (leía JSON inexistente); copia ejercicios de la última sesión completada del tipo.
- **Fix crítico** — back desde PostSesión: `volverDesdePostSesion()` va a LISTA si la sesión es COMPLETADA, a EN_CURSO si sigue EN_CURSO (evitaba mostrar "Cancelar" en sesiones ya cerradas).

---

### T-017 — Generador de próxima sesión con IA
**Commit**: hecho

**Estado**: build verde, 73 tests verdes, probado en móvil ✓ (2026-05-30).

- `domain/model/Enums.kt` — `TipoSesion` eliminado como enum. `EstadoSesion`: PREPARADA (renombrado de PLANIFICADA), EN_CURSO, COMPLETADA, OMITIDA. `EstadoSerie`: COMPLETADA, OMITIDA. `MotivoOmision`: TIEMPO, INNECESARIA, MOLESTIA. `OrigenSesion`: MANUAL, IA.
- `data/db/entity/TipoSesion.kt` — nueva entidad Room (`tableName="tipo_sesion"`): id, nombre, descripcion, esSeed, activo.
- `data/db/entity/Sesion.kt` — `tipoSesionId: Long` (FK), `fechaEjecutada: Instant?` (null hasta EN_CURSO), `estado: EstadoSesion`, `generadaPor: OrigenSesion`, notasIA, notasGlobales, rirGlobal.
- `data/db/entity/Serie.kt` — `estado: EstadoSerie`, `motivoOmision: MotivoOmision?`, `repsReales/cargaKg/rir` nullable.
- `data/db/Converters.kt` — converters para `EstadoSerie` y `MotivoOmision`.
- `data/db/dao/TipoSesionDao.kt` — CRUD completo: insert, update, delete, getAll, getActivos, getById, count.
- `data/db/dao/SesionDao.kt` — getActivas() (EN_CURSO+PREPARADA ordenadas), getPendientes(), getPreparadaByTipo(), getCompletadasByTipo(limit).
- `data/db/RecompoDatabase.kt` — versión 3, entidad TipoSesion añadida, `tipoSesionDao()`.
- `App.kt` — `seedTiposSesionIfEmpty()`: inserta A/B/C con esSeed=true si la tabla está vacía.
- `domain/usecase/GenerarSesionUseCase.kt` — llamada a Claude con tool `proponer_sesion`; contexto con historial de las 3 últimas sesiones COMPLETADAS del tipo, notas de molestias, equipamiento y deload; fallback automático si la API falla (repite última sesión +1 rep donde RIR≥3).
- `assets/tools/generar_sesion.json` — schema del tool `proponer_sesion`.
- `ui/entreno/EntrenoViewModel.kt` — reescrito: `SesionConTipo`, flujo IA (abrirNuevaSesion → iniciarGeneracionIA → propuestaSesion), gestión de tipos, aceptar/regenerar/descartar propuesta, serie OMITIDA con MotivoOmision.
- `ui/entreno/EntrenoScreen.kt` — reescrito: 4 fases (LISTA/PRE_SESION/EN_CURSO/POST_SESION), `PropuestaCard` inline, `DialogGestionTipos`, `DialogSaltarSerie` con FilterChip de motivo.
- `ui/home/HomeViewModel.kt` — usa `sesionDao.getActivas()`, resuelve `tipoNombre` via `tipoSesionDao.getById()` dentro del combine (suspend), `sesionActiva + sesionActivaTipoNombre` en UiState.
- `ui/home/HomeScreen.kt` — `TarjetaSesion(sesion, tipoNombre)`, `TarjetaIA` cuando no hay sesión activa con CTA a Entreno.
- `MainActivity.kt` — `HomeViewModelFactory` con `tipoSesionDao`, `EntrenoViewModelFactory` con `GenerarSesionUseCase`.
- `data/backup/BackupDto.kt` — versión 2: `TipoSesionDto`, `SesionDto` con `tipoSesionId` (sin `tipo`/`fechaPrevista`), `SerieDto` con nullable fields y `motivoOmision`.
- `data/backup/BackupSerializer.kt` + `XlsxExporter.kt` — referencias al schema nuevo.
- `data/ai/ParseComidaUseCase.kt` — `android.util.Log` reemplazado por Timber (fix test).
- `test/SesionDaoTest.kt` — reescrito para nuevo schema.
- `test/TipoSesionDaoTest.kt` — nuevo, 8 tests CRUD.

---

### T-014 — Cliente Claude API
**Commit**: pendiente

**Estado**: build verde, 57 tests verdes (+13 DeloadCalendarTest) (2026-05-29).

- `domain/ai/ContextLimits.kt` — constantes MAX_SESIONES_HISTORICO=3, MAX_MEDICIONES_HISTORICO=4, MAX_MENSAJES_CHAT=20, MAX_PROMPT_CHARS=8000.
- `domain/ai/DeloadCalendar.kt` — semanas 11/17/23 hardcodeadas; `isDeloadWeek()`, `daysToNextDeload()`, `deloadContextText()` para inyectar contexto en el prompt.
- `data/ai/ClaudeModels.kt` — `DEFAULT_MODEL = "claude-sonnet-4-6"`.
- `data/ai/dto/ClaudeRequest.kt` — `ClaudeRequest`, `Message`, `Tool` (@Serializable).
- `data/ai/dto/ClaudeResponse.kt` — `ClaudeResponse`, `ContentBlock`, `Usage` (@Serializable, ignoreUnknownKeys, explicitNulls=false).
- `data/ai/ClaudeApi.kt` — Retrofit interface `POST v1/messages`.
- `data/ai/ClaudeClient.kt` — OkHttp con ApiKey interceptor + Anthropic-Version header; logging solo en debug; timeouts 30s connect / 60s read.
- `assets/prompts/system_base.txt` — template de perfil con marcadores {NOMBRE}, {EDAD}, {KCAL_OBJETIVO}, etc.
- `assets/prompts/system_sesion.txt` — reglas de progresión plan v2 completas + marcadores {EQUIPAMIENTO}, {DELOAD_CONTEXTO}, {HISTORIAL_SESIONES}.
- `App.kt` — añadido `claudeApi by lazy { ClaudeClient.create(...) }`.
- 13 tests DeloadCalendar (isDeloadWeek, daysToNextDeload, deloadContextText — semanas 11/17/23 y casos borde).

---

## DONE (reciente)

### T-013 — Smoke test MVP completo
**Estado**: completado (2026-05-29). Los 9 pasos del checklist pasaron sin bugs bloqueantes. MVP Fase 1 cerrado.

---

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

---

### T-018 — Chat conversacional (nutricional + entreno)
**Estimación**: 4-5h
**Fase**: 2

**Descripción**
Pantalla de chat bajo demanda (acceso desde Home o desde las pantallas de Nutrición/Entreno). Conversación persistida en `Conversacion` + `MensajeIA`. System prompt construido dinámicamente con contexto del día (macros consumidos, sesión de hoy, últimas 4 mediciones) desde `system_base.txt`. Límites: `MAX_MENSAJES_CHAT = 20`, `MAX_PROMPT_CHARS = 8000` con truncado por lo más antiguo.

Sin historial visual de conversaciones anteriores en MVP (datos guardados pero sin pantalla de listado).

**Hecho cuando**
- Chat accesible desde Home
- Mensajes persistidos en BD
- Contexto del día inyectado en system prompt
- Truncado automático si se excede límite
- Mensaje de error claro si la API falla

---

### T-019 — Pantalla "Uso de IA" en Settings
**Estimación**: 2h
**Fase**: 2

**Descripción**
Card en Settings (ya hay un placeholder) con: tokens medios por llamada en la última semana (leídos de `MensajeIA.tokensIn/Out`), número de llamadas, estimación de coste en euros (Sonnet 4.6: $3/M input + $15/M output). Aviso si los tokens medios suben sostenidamente (posible prompt hinchado).

**Hecho cuando**
- Card visible con métricas reales de la semana
- Estimación de coste calculada correctamente
- Sin regresiones en Settings

---

### T-020 — Gráficas de progreso
**Estimación**: 4-5h
**Fase**: 3

**Descripción**
Pantalla nueva o sección en Mediciones con gráficas de evolución: peso, % grasa, masa magra, kcal medias por semana. Librería Vico (ya en el plan técnico). Rango temporal seleccionable (1 mes / 3 meses / todo).

---

### T-021 — Entreno versión completa
**Estimación**: 6-8h
**Fase**: 3

**Descripción**
Mejoras sobre la versión simple actual: timer de descanso entre series configurable con notificación al terminar, `FLAG_KEEP_SCREEN_ON` durante la sesión, recuperación robusta tras rotación / app en background, animaciones y RIR slider con feedback háptico.

---

### T-022 — Historial de conversaciones IA
**Estimación**: 2-3h
**Fase**: 3

**Descripción**
Pantalla de listado de conversaciones anteriores (ya guardadas en BD desde T-018). Lista por fecha, título auto-generado desde primera frase. Permite releer pero no continuar conversaciones pasadas.

---

### T-023 — Notificaciones
**Estimación**: 2-3h
**Fase**: 3

**Descripción**
Recordatorio mensual de tomar medición. Aviso de semana de deload próxima (basado en `DeloadCalendar`). Notificación de timer de descanso entre series (requiere T-021).

---

### T-024 — Polish UI + fotos en mediciones
**Estimación**: 4-6h
**Fase**: 3

**Descripción**
Fotos opcionales en mediciones (URI persistente con scoped storage). Animaciones de transición entre pantallas. Refinamiento visual general. Solo si la app lleva 2+ meses de uso activo.

---

### T-016 — Catálogo de ingredientes + plantillas-receta
**Estimación**: 6-8h (bloque grande, hacer en una sola sesión)
**Prioridad**: alta — desbloquea parseo de unidades con valores reales y plantillas editables

#### Contexto de partida
- T-015 (Parser IA) completado: flujo 2 fases (pregunta opcional → resultado con supuestos).
- `ComidaBase` ya existe con totales directos (kcal/prot/grasa/carbo). Las 7 plantillas del seed siguen funcionando sin cambios.
- `app/src/main/assets/seed/ingredientes_seed.json` ya creado con 41 ingredientes listos para seed.
- Schema Room actual: versión vigente en `RecompoDatabase`. Al añadir tablas, subir versión y añadir migración (o usar destructive, que está activo).

#### Parte 1 — Modelo de datos nuevo (Room)

**Entidad `Ingrediente`**:
```
id: Long (PK autoincrement)
nombre: String
kcal100g: Double
prot100g: Double
grasa100g: Double
carbo100g: Double
gramosPorUnidad: Int?       (null = a granel, solo gramos)
nombreUnidad: String?       (null si a granel; ej: "cazo", "loncha", "natilla")
fiabilidad: Fiabilidad      (enum: VALIDADO, ESTIMADO)
activo: Boolean = true
```

**Entidad `PlantillaIngrediente`** (tabla intermedia ComidaBase ↔ Ingrediente):
```
id: Long (PK autoincrement)
comidaBaseId: Long (FK → ComidaBase.id)
ingredienteId: Long (FK → Ingrediente.id)
cantidadG: Double
```

**`ComidaBase`**: sin cambios en la entidad. Una `ComidaBase` "compuesta" se detecta porque tiene filas en `PlantillaIngrediente`. Las "simples" (solo totales directos) siguen funcionando igual.

**DAOs necesarios**:
- `IngredienteDao`: `insert`, `getAll()`, `count()`, `search(query)` (LIKE para autocompletado), `getById(id)`, `update`
- `PlantillaIngredienteDao`: `insertAll(lista)`, `getByComidaBase(comidaBaseId): Flow<List<PlantillaIngrediente>>`, `deleteByComidaBase(id)`, `upsert`

**Migración Room**: subir `DATABASE_VERSION` +1. Añadir migración que crea `ingrediente` y `plantilla_ingrediente`. Alternativa: destructive sigue activo pero perderá datos — decidir al inicio según si hay datos reales que conservar.

#### Parte 2 — Seed de ingredientes

Mismo patrón idempotente que ejercicios (en `App.kt`):
- `seedIngredientesIfEmpty()`: si `ingredienteDao.count() == 0`, leer `assets/seed/ingredientes_seed.json`, parsear con `org.json.JSONArray` (ya disponible), insertar. El campo `nota` puede ignorarse o guardarse si se añade columna `nota: String?`.
- El JSON ya está en `assets/seed/ingredientes_seed.json`. Campos: `nombre`, `kcal100g`, `prot100g`, `grasa100g`, `carbo100g`, `gramosPorUnidad` (nullable), `nombreUnidad` (nullable), `fiabilidad` ("VALIDADO"/"ESTIMADO"), `nota`.
- `fiabilidad` se mapea a enum `Fiabilidad.valueOf(str.uppercase())`.

#### Parte 3 — Las 3 funcionalidades

**3a. Autocompletado desde historial** (en el campo "Descripción" del dialog de nutrición):
- Al escribir ≥2 caracteres, consultar `entradaComidaDao.search(normalizado(texto))` (query LIKE `%texto%` en `textoLibre`).
- Normalización: minúsculas + quitar tildes (`java.text.Normalizer`) + trim. Ordenar por más reciente.
- Mostrar hasta 5 sugerencias como `DropdownMenu` bajo el campo.
- Al seleccionar: rellenar textoLibre, kcal, prot, grasa, carbo desde la entrada histórica. Marcar `parseadaPorIA = entrada.parseadaPorIA`.
- En el DAO: añadir `fun search(query: String): Flow<List<EntradaComida>>` con `SELECT * FROM entrada_comida WHERE textoLibre LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT 5`.

**3b. Botón "Guardar como plantilla"** (disponible en fase de resultado IA y en fase de edición manual si hay valores):
- Aparece en `ResultadoIACard` y también en el dialog normal cuando hay textoLibre + macros rellenos.
- Al pulsar: dialog de confirmación con campo nombre (pre-rellenado con `textoLibre`) y selector de slot.
- Lógica: intentar descomponer en ingredientes del catálogo (buscar cada palabra del nombre en `Ingrediente`; si hay coincidencia, preguntar la cantidad). Si no hay coincidencias → guardar `ComidaBase` con totales directos.
- Para la primera versión: guardar siempre con totales directos (sin ingredientes) — la descomposición puede ser Momento 2.

**3c. Plantillas-receta editables** (en el modo Plantilla del dialog, cuando la ComidaBase seleccionada tiene `PlantillaIngrediente`):
- Si `comidaBaseId` tiene ingredientes en `PlantillaIngrediente`, mostrar lista de ingredientes con cantidades editables.
- Cada ingrediente: nombre + campo numérico de gramos (o unidades si `gramosPorUnidad != null`).
- Al cambiar cualquier cantidad: recalcular totales en tiempo real: `kcal = Σ (ingrediente.kcal100g * cantidad / 100)`.
- Botón "Usar esta combinación": guarda como `EntradaComida` con los totales recalculados.

#### Parte 4 — Alta de ingredientes desde la app

Cuando el parser IA calcula una comida y el texto menciona un alimento no reconocido en el catálogo:
- Mostrar chip/botón "¿Añadir '[nombre]' al catálogo?".
- Dialog de alta: nombre (pre-rellenado), campos kcal/prot/grasa/carbo por 100g (pre-rellenados desde lo que estimó la IA), gramosPorUnidad (opcional), nombreUnidad (opcional).
- La IA estima los valores por 100g desde el texto: añadir instrucción al prompt de `ParseComidaUseCase` para que también devuelva `ingrediente_nuevo: {nombre, kcal100g, prot100g, grasa100g, carbo100g}` cuando detecta un alimento no estándar (ajuste menor al schema del tool).
- El usuario valida/corrige y guarda → `Ingrediente` con `fiabilidad = ESTIMADO`.

#### Parte 5 — Interpretación de unidades en el parser (refuerzo del prompt)

Cuando el usuario diga "una natilla", "un cazo de whey", "3 lonchas de pavo":
- Antes de llamar a la IA: buscar en `Ingrediente` si algún nombre coincide con el texto.
- Si hay coincidencia con `gramosPorUnidad != null`: inyectar en el prompt la equivalencia. Ej: "Nota: 'natilla proteica Mercadona' = 120g por unidad, kcal/100g=108, prot/100g=12.5".
- La IA usa esos valores en lugar de genéricos. Esto mejora la confianza del resultado sin cambiar el flujo.
- Implementación: helper `resolverUnidades(texto, ingredientes): String` que devuelve el texto + las notas de contexto a inyectar.

#### Orden de implementación sugerido
1. Room: entidades + DAOs + migración + seed → build verde
2. Autocompletado (más visible, útil de inmediato)
3. "Guardar como plantilla" (totales directos primero)
4. Plantillas-receta editables (requiere que haya ComidaBase con ingredientes)
5. Alta de ingredientes desde la app
6. Interpretación de unidades en el parser

#### Hecho cuando
- Build verde, tests verdes (añadir al menos test de IngredienteDao y cálculo de totales de plantilla)
- Seed de ingredientes funciona: primera instalación carga los 41
- Autocompletado muestra sugerencias al escribir en el campo descripción
- "Guardar como plantilla" crea ComidaBase y aparece en el dropdown
- Plantilla con ingredientes muestra lista editable con recálculo en vivo
- Sin regresiones en T-015 (parser IA sigue funcionando)

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

### DT-005 — Forzar deload manualmente
**Estado**: abierta. Hoy no hay forma de decirle a la app "esta semana es deload aunque el calendario diga semana X". Si se adelanta, generar la sesión y editarla a mano antes de aceptar. Mover fechas de deload a Settings cuando se necesite.



### DT-001 — Fechas en formato español + DatePicker
**Estado**: ✅ resuelta en T-009 iter 2 (DatePicker de Material 3 con locale "es" introducido en "Nueva sesión"). El wizard sigue con input ISO, pero ya hay patrón reutilizable cuando se aborde Settings (T-011).

### DT-002 — Seed de plantillas de comida
**Estado**: ✅ resuelta en T-012 (seed de 7 plantillas reales de Vic: 1 desayuno, 3 tostadas, 3 natillas).

### DT-003 — Fecha en nombre del fichero JSON de backup
**Estado**: ✅ resuelta. **Commit**: hecho (2026-05-29).
`hacerBackup()` ahora escribe `recomposicion_YYYY-MM-DD.json`. Cada día genera un fichero nuevo en Drive, sin sobreescribir el anterior.

### DT-004 — Separar objetivo nutricional y gasto real
**Estado**: ✅ resuelta. **Commit**: hecho (2026-05-29). Build verde, tests verdes.

- `UserSettings`: `kcalBaseDia` → `kcalDescanso / kcalMusculacion / kcalBici` + `metabolismoBasalKcal` (default 1830).
- `UserSettingsStore`: 4 nuevas claves DataStore. ⚠️ Al instalar, el wizard vuelve a aparecer — reconfigurar macros.
- `CalcularKcalObjetivoUseCase`: `calcularObjetivoNutricional(tipoDia, ...)` + `calcularGastoReal(basal, actividades)`.
- `HomeViewModel`: TipoDia auto con prioridad BICI > MUSCULACION > DESCANSO. Override manual vía `setTipoDia()`.
- `HomeScreen`: chip TipoDia clickable. `TarjetaObjetivoNutricional` + `TarjetaGasto` (déficit real).
- `WizardScreen/ViewModel`: 3 campos kcal + metabolismo basal en paso 2.
- `SettingsScreen/ViewModel`: 4 items editables en CardMacros.
- Wizard paso 2: 3 campos de kcal en lugar de 1.

**Cuando atacar**: de golpe, antes de Fase 2 o cuando el uso diario muestre que el número confunde. Impacta wizard, settings, home y el use case — atacar todo en una sola tarea.

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
