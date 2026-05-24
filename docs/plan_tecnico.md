# PLAN TÉCNICO — APP DE RECOMPOSICIÓN CORPORAL

**José Víctor Sánchez Riquelme**
Fecha: 24/05/2026
Versión: 1.6 (patch ADR-0004: cálculo dinámico de kcalObjetivo basado en actividad)

---

## 0. DECISIONES CERRADAS

| Decisión | Valor |
|---|---|
| Plataforma | Android nativo |
| Lenguaje / UI | Kotlin + Jetpack Compose |
| Persistencia | Room (SQLite local), un único módulo `:app` |
| Inyección dependencias | Constructor injection manual + ViewModelFactory simple. **Sin Hilt.** |
| Motor de IA | Claude API (Anthropic). Constante `DEFAULT_MODEL = "claude-sonnet-4-6"`. |
| Distribución | APK firmado, sideload |
| Auth / multiusuario | No |
| Backup | JSON continuo + export XLSX bajo demanda (no espejo continuo) |
| Wizard inicial | Sí (3 pantallas) en primer arranque |
| Fotos de medición | **Fuera del MVP**. Vuelven en Fase 3 si la app sigue viva. |
| Modelo de desarrollo | Vibe coding con Claude Code. Proceso ligero. |
| Backlog | `PROGRESS.md` plano con estados TODO / DOING / DONE |
| Definition of Done | Build verde + tú lo pruebas en el móvil + commit limpio |
| Auditoría IA externa | Informal: pegar a otra IA cuando algo huela raro |
| ADRs | Solo 3 fundacionales (stack, local-only, IA) |

---

## 1. ARQUITECTURA

### 1.1 Estructura del código

Un solo módulo Gradle (`:app`). Modularización por paquetes:

```
com.vic.recompo/
├── data/           → Room entities, DAOs, repositorios, cliente Claude API
├── domain/         → modelos puros, cálculos (Navy, IMC, balance kcal), reglas de progresión hardcoded
├── ui/             → pantallas Compose + ViewModels
│   ├── home/
│   ├── nutricion/
│   ├── entreno/
│   ├── mediciones/
│   ├── actividad/
│   ├── settings/
│   └── common/     → componentes reutilizables (MacroBar, RirSlider, etc.)
└── App.kt          → entrypoint, MainActivity, NavHost
```

Patrón: MVVM con ViewModels + `StateFlow<UiState>` + sealed class `Event` para acciones one-shot. Los repositorios son clases normales, instanciadas en `App.kt` y pasadas a las factories de ViewModel.

**Justificación de no usar Hilt**: Hilt aporta valor con muchos módulos y muchos ViewModels. Aquí son ~6-8 ViewModels. Construirlos a mano con `ViewModelFactory` es más simple, más rápido de compilar, y elimina una capa de magia.

### 1.2 Bibliotecas

| Función | Librería |
|---|---|
| UI | Jetpack Compose + Material 3 (BOM última estable) |
| Navegación | Compose Navigation |
| Persistencia | Room + KSP |
| Settings | DataStore Preferences |
| HTTP | OkHttp + Retrofit + kotlinx.serialization |
| Coroutines | kotlinx-coroutines |
| Logging | Timber (debug only) |
| Testing | JUnit4 + Robolectric + Turbine |
| Gráficas (Fase 3) | Vico |

JUnit4 + Robolectric en lugar de JUnit5: estándar en Android, sin fricción con Hilt (que no usamos), Compose UI test y Room in-memory.

---

## 2. MODELO DE DATOS

### 2.1 Perfil de usuario (DataStore)

```kotlin
data class UserSettings(
    // Datos personales
    val nombre: String,
    val fechaNacimiento: LocalDate,
    val sexo: Sexo,
    val alturaCm: Int,

    // Plan
    val fechaInicioPlan: LocalDate,
    val pesoInicialKg: Double,
    val pesoObjetivoKg: Double,
    val faseActual: String,            // texto libre: "Fase 1 casa", "Fase 2 gym"

    // Macros objetivo (cambia cuando cambias de fase: editas aquí)
    val kcalBaseDia: Int,              // 1900 — déficit de mantenimiento
    val proteinaObjetivoG: Int,        // 160

    // Backup
    val carpetaBackupUri: String?,
    val ultimoBackupOk: Instant?,      // null si nunca ha funcionado
    val ultimoBackupError: String?,    // mensaje si el último intento falló
    val ultimoBackupBytes: Long?       // tamaño escrito (para detectar 0 bytes silencioso)
)
```

**Por qué los macros viven aquí y no en una entidad `Fase`**: una sola persona, una sola fase activa a la vez. Cuando cambies de fase 1 a fase 2 (1 jun), editas estos campos en Settings. Si en el futuro quieres histórico de fases, se modela entonces. **No anticipes**.

**Cálculo dinámico de `kcalObjetivo` (ADR-0004)**: el objetivo de kcal del día se calcula en runtime como:

```
kcalObjetivo = kcalBaseDia + sum(actividadesHoy.kcalQuemadas)
```

Esto refleja la realidad (45 min de paseo ≠ 90 min de salida) y evita un esquema rígido con tres valores fijos por tipo de día. `TipoDia` (MUSCULACION / BICI / DESCANSO) se mantiene como **etiqueta visual informativa** en Home, pero **no afecta al cálculo** del objetivo. La implementación vive en `domain/usecase/CalcularKcalObjetivoUseCase.kt`.

### 2.2 Comida base (plantillas reutilizables)

```kotlin
@Entity
data class ComidaBase(
    @PrimaryKey val id: Long,
    val slot: SlotComida,              // DESAYUNO, ALMUERZO, COMIDA, MERIENDA, CENA
    val variante: String,              // "Atún", "Lomo embuchado", "Natilla + plátano"
    val kcal: Int,
    val proteinaG: Double,
    val grasaG: Double,
    val carboG: Double,
    val ingredientesTexto: String,     // descripción de gramos
    val activo: Boolean
)
```

Sin `faseId`. Sin `tipoDiaFiltro`. Cuando cambies de fase, editas las plantillas o creas variantes nuevas. El log diario referencia opcionalmente una plantilla pero no depende de ella.

### 2.3 Catálogo de ejercicios

```kotlin
@Entity
data class Ejercicio(
    @PrimaryKey val id: Long,
    val nombre: String,
    val grupoMuscularPrincipal: GrupoMuscular,
    val gruposSecundarios: String,     // CSV: "TRICEPS,HOMBRO_FRONTAL" — string simple, no JSON
    val patron: PatronMovimiento,
    val equipamientoCasa: Boolean,
    val equipamientoGym: Boolean,
    val notasTecnica: String?,
    val activo: Boolean
)
```

`gruposSecundarios` como CSV en lugar de JSON o tabla relacional: para una app personal, queries tipo "ejercicios que trabajen tríceps" son raras. Si llega el caso, parseas el CSV. Cero overhead.

**Advertencia explícita**: NO construyas lógica compleja sobre este campo. Mientras se use para mostrar visualmente, filtrar simple o sugerir, el CSV es perfecto. Si te ves tentado de hacer queries SQL con `LIKE '%TRICEPS%'` o joins implícitos, párate: significa que ha llegado el momento de promoverlo a tabla relacional, no de complicar el código encima del CSV.

### 2.4 Sesiones de entrenamiento

Una sola entidad `Sesion`. Sin distinción entre "planificada" y "realizada": una sesión existe como planificada (campos de objetivo) y al ejecutarse rellena los campos reales.

```kotlin
@Entity
data class Sesion(
    @PrimaryKey val id: Long,
    val tipo: TipoSesion,              // A, B, C
    val fechaPrevista: LocalDate,
    val fechaEjecutada: Instant?,      // null = aún no ejecutada
    val estado: EstadoSesion,          // PLANIFICADA, EN_CURSO, COMPLETADA, OMITIDA
    val generadaPor: OrigenSesion,     // SEED, IA, MANUAL
    val notasIA: String?,              // razonamiento de la IA al proponer
    val notasGlobales: String?,        // tu nota libre al cerrar
    val rirGlobal: Int?                // RIR percibido de la sesión, 0-5
)

@Entity
data class EjercicioEnSesion(
    @PrimaryKey val id: Long,
    val sesionId: Long,
    val ejercicioId: Long,
    val orden: Int,
    // Plan
    val seriesObjetivo: Int,
    val repsObjetivoMin: Int,
    val repsObjetivoMax: Int,
    val cargaObjetivoKg: Double?,
    val notas: String?
)

@Entity
data class Serie(
    @PrimaryKey val id: Long,
    val ejercicioEnSesionId: Long,
    val numero: Int,
    val repsReales: Int,
    val cargaKg: Double,
    val rir: Int,                      // 0-5
    val completada: Boolean
)
```

**RIR solo, sin RPE**. Son métricas redundantes (RIR ≈ 10 − RPE). Pides una.

**Sin campo "volumen total"**. Si lo necesitas para una gráfica, lo calculas a partir de las series. Para ejercicios sin carga (plancha, dead bug, peso corporal) el volumen como reps×kg no aplica; se calcula sólo donde tiene sentido.

### 2.5 Log diario

```kotlin
@Entity
data class EntradaComida(
    @PrimaryKey val id: Long,
    val fecha: LocalDate,
    val slot: SlotComida,
    val textoLibre: String,
    val kcal: Int,
    val proteinaG: Double,
    val grasaG: Double,
    val carboG: Double,
    val comidaBaseId: Long?,           // null si fue parseado por IA o entrada libre
    val parseadaPorIA: Boolean,
    val timestamp: Instant
)

@Entity
data class Actividad(
    @PrimaryKey val id: Long,
    val fecha: LocalDate,
    val tipo: String,                  // "bici", "caminata", "otro" (texto libre)
    val descripcion: String?,
    val duracionMin: Int?,
    val kcalQuemadas: Int              // input manual obligatorio
)
```

### 2.6 Mediciones

```kotlin
@Entity
data class Medicion(
    @PrimaryKey val id: Long,
    val fecha: LocalDate,
    // Crudas
    val pesoKg: Double,
    val cinturaCm: Double?,
    val caderaCm: Double?,
    val cuelloCm: Double?,
    val pechoCm: Double?,
    val bicepsCm: Double?,
    val musloCm: Double?,
    // Snapshot + derivados (calculados al crear, persistidos como valores)
    val alturaCmEnLaMedicion: Int,
    val grasaPct: Double?,             // Navy
    val grasaPctOverride: Boolean,     // true si lo metiste a mano
    val masaGrasaKg: Double?,
    val masaMagraKg: Double?,
    val imc: Double?,
    val whr: Double?,
    // Contexto
    val faseTexto: String?,            // snapshot de UserSettings.faseActual
    val hito: String?,                 // "Inicio", "94→91", etc.
    val notas: String?
)
```

**Sin fotos en MVP**. Las fotos en Android son trabajo invisible (URI persistentes, scoped storage, EXIF, backup, tamaños). Volverán en Fase 3 si la app sigue viva.

### 2.7 Conversaciones IA

```kotlin
@Entity
data class Conversacion(
    @PrimaryKey val id: Long,
    val tipo: TipoConversacion,        // NUTRICIONAL, ENTRENO
    val fechaCreacion: Instant,
    val titulo: String?                // auto-generado o primera frase del usuario
)

@Entity
data class MensajeIA(
    @PrimaryKey val id: Long,
    val conversacionId: Long,
    val rol: RolMensaje,               // USER, ASSISTANT
    val contenido: String,
    val tokensIn: Int?,
    val tokensOut: Int?,
    val timestamp: Instant
)
```

Sin `contextoJson` snapshot. El contexto del día (macros, sesiones, mediciones) se reconstruye dinámicamente al enviar cada mensaje. Persistir el snapshot inicial era sobreingeniería.

**MVP**: las conversaciones se persisten siempre (coste: 2 entidades + un insert por mensaje, trivial), pero **no hay pantalla de historial**. El chat es en vivo: al cerrar la pantalla, la conversación queda guardada pero solo consultable vía export. Pantalla de historial en Fase 3.

### 2.8 Notas de schema

- **IDs**: `Long` autogenerado.
- **Fechas**: `LocalDate` para días, `Instant` para timestamps. Type converters obligatorios.
- **Enums**: type converter a String, nunca ordinal.
- **Migraciones**: `fallbackToDestructiveMigration()` permitido al principio. **Criterio para desactivarlo**: cuando tengas 2-3 semanas de datos reales registrados que duela perder (mediciones, sesiones de entreno, comidas). No es una fecha del calendario: es cuando abras la app, mires los datos y pienses "no quiero perder esto". A partir de ahí, migraciones manuales obligatorias y backup explícito antes de cualquier migración.
- **Soft delete**: flag `activo` en `Ejercicio` y `ComidaBase`.

---

## 3. INTEGRACIÓN CON CLAUDE API

### 3.1 Gestión de la API key

`BuildConfig` desde `local.properties` (gitignored):

```properties
CLAUDE_API_KEY=sk-ant-...
```

```kotlin
// build.gradle.kts (app)
buildConfigField(
    "String",
    "CLAUDE_API_KEY",
    "\"${gradleLocalProperties(rootDir).getProperty("CLAUDE_API_KEY") ?: ""}\""
)
```

Riesgo de "alguien abre el APK y saca la key": teóricamente real, prácticamente cero (APK vive sólo en tu móvil).

### 3.2 Modelo y cliente

```kotlin
object ClaudeModels {
    const val DEFAULT_MODEL = "claude-sonnet-4-6"
}

interface ClaudeApi {
    @POST("v1/messages")
    suspend fun messages(@Body request: ClaudeRequest): ClaudeResponse
}

data class ClaudeRequest(
    val model: String = ClaudeModels.DEFAULT_MODEL,
    @SerialName("max_tokens") val maxTokens: Int = 4096,
    val system: String? = null,
    val messages: List<Message>,
    val tools: List<Tool>? = null
)
```

Modelo en constante con nombre, no como literal repetido. Si Anthropic cambia el nombre o despreca el modelo, cambias un sitio.

### 3.3 Casos de uso

| # | Caso | Modelo | Cuándo |
|---|---|---|---|
| 1 | Parsear comida texto libre | Sonnet 4.6 | Al registrar comida que NO encaja con plantilla |
| 2 | Generar próxima sesión | Sonnet 4.6 | Al cerrar sesión de entreno |
| 3 | Chat nutricional / entreno | Sonnet 4.6 | Bajo demanda |

**Importante** — caso 1: si la entrada coincide con una plantilla (`ComidaBase`), se usa la plantilla directamente, sin llamar a la IA. Esto ahorra ~70% de llamadas en una semana típica.

### 3.4 Tool calling

Para casos 1 y 2 (JSON estructurado), tool calling. Schemas en `assets/tools/` como JSON literales:

`assets/tools/registrar_comida.json`:

```json
{
  "type": "object",
  "properties": {
    "kcal": { "type": "integer" },
    "proteina_g": { "type": "number" },
    "grasa_g": { "type": "number" },
    "carbo_g": { "type": "number" },
    "confianza": {
      "type": "string",
      "enum": ["alta", "media", "baja"]
    },
    "notas": { "type": "string" }
  },
  "required": ["kcal", "proteina_g", "grasa_g", "carbo_g"]
}
```

Carga en Kotlin:

```kotlin
val schema = context.assets
    .open("tools/registrar_comida.json")
    .bufferedReader().use { it.readText() }

val tool = Tool(
    name = "registrar_comida",
    description = "Registra los macros calculados de una comida",
    inputSchema = Json.parseToJsonElement(schema).jsonObject
)
```

### 3.5 Prompts

System prompt común en `assets/prompts/system_base.txt`. Plantilla con marcadores que la app sustituye en runtime:

```
Eres asistente del plan de recomposición corporal de Vic.
Datos: 46 años, 180 cm, fase actual: {FASE}.
Plan: déficit moderado, 160g proteína mínimo, deload cada 5-6 semanas.
Hombro con historial: precaución con press militar y empuje vertical.

Contexto dinámico hoy:
- Últimas 4 mediciones: {MEDICIONES}
- Macros del día: consumido {KCAL_HOY}/{KCAL_OBJETIVO}, prot {PROT_HOY}/{PROT_OBJETIVO}
- Última sesión {TIPO}: {RESUMEN}

Estilo: directo, números, español, sin paja.
```

### 3.6 Límites de contexto

Los prompts crecen sin querer cuando reconstruyes contexto dinámicamente. Constantes duras desde el día 1, en `domain/ai/ContextLimits.kt`:

```kotlin
object ContextLimits {
    const val MAX_SESIONES_HISTORICO = 3       // últimas 3 sesiones del mismo tipo (A/B/C)
    const val MAX_MEDICIONES_HISTORICO = 4     // últimas 4 mediciones
    const val MAX_MENSAJES_CHAT = 20           // últimos 20 mensajes en chat largo
    const val MAX_PROMPT_CHARS = 8000          // límite global del system + user
}
```

**Regla de truncado**: si construyendo el contexto se sobrepasa `MAX_PROMPT_CHARS`, se trunca empezando por lo más antiguo (mediciones primero, luego sesiones, luego mensajes de chat). Cada vez que se trunca, log en Timber con `Timber.w("Context truncated: %d chars over limit", excess)` para detectar prompts hinchados.

Pantalla "Uso de IA" en Settings muestra: tokens medios por llamada en última semana. Si suben sostenidamente, alguno de los límites está mal calibrado.

### 3.7 Contingencia si la API falla

Si Claude API timeoutea o devuelve error en momento crítico:
- **Parseo de comida**: cae a entrada manual (inputs de kcal/prot/grasa/carbo a pelo). El usuario completa.
- **Generación de sesión**: cae a "repetir sesión anterior con +1 rep en cada ejercicio donde RIR ≥ 3". Regla simple hardcoded.
- **Chat**: mensaje "Sin conexión a Claude ahora, reintenta en un momento".

### 3.8 Coste estimado

Sonnet 4.6, semana típica:
- ~10-15 parseos reales de comida (el resto cae a plantilla local) → fracciones de céntimo
- 3 generaciones de sesión → ~5-10 céntimos
- 5-15 mensajes de chat → variable, ~10-20 céntimos

**Estimación honesta: 5-10 €/mes**. Despreciable, pero no <2 € como dije antes.

Loggea `tokens_in` y `tokens_out` en `MensajeIA`. Pantalla en Settings → "Uso de IA" muestra gasto del mes.

---

## 4. PANTALLAS Y FLUJOS

### 4.1 Mapa

```
Home (dashboard del día + chip estado backup)
├── Nutrición — log + chat IA (sin historial visual en MVP)
├── Entrenamiento — sesión actual (simple), histórico
├── Actividad — log de kcal quemadas
├── Mediciones — lista + nueva
└── Settings — perfil, plantillas, backup (con estado), uso IA
```

### 4.2 Flujos críticos

**Primer arranque (wizard de 3 pantallas)**

1. Datos personales: nombre, fecha nacimiento, sexo, altura.
2. Plan: fecha inicio, peso inicial, peso objetivo, macros objetivo de los 3 tipos de día.
3. Backup: elegir carpeta Drive vía SAF (skippable, configurable después).

**Registrar comida**

1. Home → tap en slot vacío.
2. Pantalla log: dropdown "elegir variante de plantilla" + input texto libre.
3. Si eliges plantilla → macros precargados, sin llamada a IA.
4. Si escribes texto libre → botón "Calcular con IA" → spinner 1-3s → macros + confianza.
5. Editar manualmente si hace falta → aceptar.

**Sesión de entreno (MVP — versión simple)**

1. Home → "Iniciar sesión A/B/C" si toca hoy.
2. Pre-sesión: lista de ejercicios con objetivos (series x reps, carga objetivo). Botón empezar.
3. En curso: **lista plana de ejercicios** con inputs por serie (reps, carga, RIR 0-5). Persistencia simple en BD al cerrar cada serie. Sin timer automático, sin gestión sofisticada de estado en rotación (la app intenta restaurar pero si se pierde algo se reintroduce a mano).
4. Post-sesión: resumen + nota libre + RIR global + botón "Generar próxima sesión".
5. IA propone próxima sesión → aceptar / editar / regenerar.

**Sesión de entreno (Fase 3 — versión completa)**

Llega solo si la app sigue viva tras 2-3 meses. Añade:
- Timer de descanso entre series, configurable, con notificación al terminar.
- `FLAG_KEEP_SCREEN_ON` durante la sesión.
- Recuperación robusta tras rotación / app en background / crash (estado parcial nunca se pierde).
- Animaciones, transiciones, RIR slider con feedback háptico.

Razón de la división: la pantalla de entreno en curso es la más compleja de toda la app. Hacerla "bien" desde el día 1 puede consumir semanas. Una versión simple cubre el 80% del valor con el 20% del esfuerzo.

### 4.3 Componentes reutilizables

`MacroBar`, `RirSlider` (0-5), `ExerciseCard`, `RestTimer` (Fase 3), `DayTypeChip`, `BackupChip` (visible en Home, muestra "Backup hace 3h ✓" o "Backup falló ⚠️").

---

## 5. FASES DE DESARROLLO

Estimación honesta con simplificaciones aplicadas. Sin Hilt, sin multi-módulo, sin proceso pesado.

### Fase 0 — Setup (4-6h)
- Proyecto Android Studio, `build.gradle.kts`, versions catalog
- Tema Material 3, navegación, estructura de paquetes
- Repo Git + CI mínima
- Archivos de proceso (CLAUDE.md, PROGRESS.md, 3 ADRs, CONVENTIONS.md ligero)

### Fase 1 — MVP sin IA (25-40h)
- Room: 8 entidades, DAOs, type converters
- DataStore UserSettings + wizard 3 pantallas
- Pantalla Home (resumen del día + chip estado backup)
- Nutrición: log manual + selector de plantillas
- Mediciones: CRUD con cálculos automáticos (Navy, IMC, masa magra, WHR)
- Actividad: log manual
- **Entreno: versión simple** (lista plana, inputs por serie, sin timer ni gestión compleja de estado)
- Backup: JSON continuo a Drive vía SAF + actualización de campos de tracking en UserSettings + botón export XLSX
- Settings básico

**Hito 1**: app utilizable sin IA. Más útil que el papel.

### Fase 2 — IA (12-20h)
- Cliente Retrofit + interceptors + manejo de errores y contingencia
- Prompts y schemas en assets
- Parser de comida (con fallback a plantilla local cuando aplica)
- Generador de próxima sesión
- Chat conversacional nutricional + entreno
- Pantalla "Uso de IA" en Settings

**Hito 2**: app asistida.

### Fase 3 — Pulido (10-15h, opcional)
- Gráficas de progreso
- Editor de plantillas de comida y catálogo de ejercicios
- **Entreno versión completa**: timer descanso, KEEP_SCREEN_ON, recuperación robusta tras rotación / crash
- **Pantalla historial de conversaciones IA**
- Notificaciones (medición mensual, deload)
- Fotos en mediciones (si tras 2 meses sigues usando la app)
- Polish UI

### Total estimado

| Fase | Min | Max |
|---|---|---|
| 0 — Setup | 4h | 6h |
| 1 — MVP sin IA | 25h | 40h |
| 2 — IA | 12h | 20h |
| 3 — Pulido | 10h | 15h |
| **Total** | **51h** | **81h** |

A 8-10h/semana → **6-10 semanas**. MVP usable en **3-5 semanas**.

---

## 6. STACK DE SOPORTE

### 6.1 Testing

Pragmático, no purista. Tests donde aportan:

- **Domain**: cálculos de grasa Navy, IMC, balance kcal, parser de respuesta IA.
- **Data**: DAOs críticos con Room in-memory.
- **UI**: solo si una pantalla rompe al refactor (sesión en curso, log de comida).

Cero tests de getters/setters, cero tests de pantallas triviales.

### 6.2 CI mínima

`.github/workflows/ci.yml`:

```yaml
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: 17, distribution: temurin }
      - uses: gradle/actions/setup-gradle@v3
      - run: ./gradlew test assembleDebug
```

Te avisa si rompes el build entre sesiones. Sin signing en CI, sin Play Store, sin nada más.

### 6.3 Distribución

Keystore generado una vez. APK firmado via `./gradlew assembleRelease`. Transfieres al móvil y instalas con "fuentes desconocidas".

### 6.4 Backup y portabilidad

**Capa 1 — JSON continuo a Drive vía SAF**

Tras cambios significativos (cierre de sesión, fin del día, abandono de pantalla con cambios pendientes), la app escribe `recomposicion.json` en la carpeta Drive elegida. Contiene todas las tablas serializadas.

JSON, no XLSX continuo. La razón: escribir XLSX cada vez es complejidad real (locking en Drive, locale, formato numérico, librería pesada). JSON es trivial y reimportable.

**Capa 2 — Export XLSX bajo demanda**

Botón en Settings → "Exportar a Excel". Genera `recomposicion_YYYY-MM-DD.xlsx` con las hojas detalladas en §6.5. Lo usas cuando quieras revisar en Excel, cuando cambies de móvil, o antes de una decisión grande.

**Capa 3 — Backup completo manual**

Botón "Exportar todo (ZIP)". JSON + base de datos SQLite cruda + XLSX en un zip. Para guardarte una copia "por si acaso" antes de experimentar.

Sin WorkManager semanal automático. Cuando quieras una copia, la creas tú. La capa 1 garantiza que nunca pierdes más de un cambio reciente.

### 6.5 Estructura del XLSX (bajo demanda)

Hojas: `Resumen`, `Mediciones`, `Entrenamiento`, `Nutrición`, `Actividad`. Misma estructura ya detallada anteriormente. Implementado con **fastexcel** sólo cuando se llama al export, no en background.

### 6.6 Logging

Timber en debug. No Crashlytics.

---

## 7. RIESGOS Y MITIGACIONES

| Riesgo | Prob | Impacto | Mitigación |
|---|---|---|---|
| Pérdida móvil → pérdida datos | Media | Alto | JSON continuo a Drive + export ZIP cuando quieras. |
| Claude API se cae o cambia | Baja | Medio | Wrapper aislado + fallback (§3.6). App sigue usable sin IA. |
| IA aluciona macros de comida | Alta | Medio | Validación de plausibilidad básica (kcal/g razonables). Confianza visible. Editable a mano siempre. |
| Migración Room rompe BD | Media | Alto | `destructiveMigration` hasta tener 2-3 sem de datos que duela perder. Desde ahí, manual + backup antes de migrar. |
| Backup silenciosamente roto (SAF, Drive, permiso invalidado) | Media | Alto | Campos `ultimoBackupOk/Error/Bytes` en UserSettings. Chip visible en Home. Si llevas >48h sin backup OK, banner de alerta. |
| Prompt IA crece sin querer (contexto hinchado) | Media | Medio | `ContextLimits` con constantes duras + truncado + log Timber al truncar. Pantalla "Uso de IA" muestra tokens medios. |
| IA propone entreno contra reglas (flexión con hombro sensible) | Media | Alto | Validación post-IA en cliente: si la propuesta viola reglas, regenera con el error como input. |
| Compose me quema (no lo conozco) | Alta | Bajo | Empezar por pantallas simples (mediciones, actividad). Sesión en curso al final. |
| Cansarme del proceso antes del valor | **Media** | **Crítico** | Plan v1.4 ya recorta esto. Si aun así pesa, parar y simplificar más. |

---

## 8. DECISIONES PENDIENTES

1. **Catálogo de ejercicios Fase 2 (gym)**: lo pueblas cuando inventaríes el gimnasio el 1 de junio.
2. **Sincronización entre dispositivos**: descartada. Si llega el caso, migrar a UUIDs.
3. **Health Connect / Strava**: descartado. Modelo `Actividad` lo soporta si llega el caso.
4. **Fotos**: vuelven en Fase 3 si la app sigue viva.
5. **Notificaciones**: Fase 3.

---

## 9. MODELO DE DESARROLLO (ligero)

### 9.1 Roles

- **Claude Code**: implementa, según `PROGRESS.md` y `CLAUDE.md`.
- **Vic**: selecciona tarea, prueba APK, mergea.
- **Otra IA (informal)**: cuando algo huela raro, le pegas el plan + lo que ha hecho Claude Code y pides opinión. Sin plantilla, sin proceso formal.

### 9.2 Archivos de proceso (6, no 14)

| Archivo | Propósito |
|---|---|
| `README.md` | Descripción del proyecto |
| `CLAUDE.md` | Contexto que Claude Code lee al iniciar cada sesión |
| `PROGRESS.md` | Backlog + estado |
| `CONVENTIONS.md` | Convenciones de código (ligero) |
| `docs/plan_tecnico.md` | Copia versionada de este plan |
| `docs/adr/0001-stack.md`, `0002-local-only.md`, `0003-claude-api.md` | 3 ADRs fundacionales |

Más `.github/workflows/ci.yml`, `.gitignore`, `local.properties.example`. Total: ~9 archivos, no 14.

### 9.3 CLAUDE.md (versión corta)

```
# CLAUDE.md

## Qué es esto
App Android personal de Vic. Tracking de recomposición corporal.
Kotlin + Compose + Room + Claude API. Un módulo, sin Hilt, local-only.

## Documentos
- docs/plan_tecnico.md — autoridad sobre QUÉ se construye
- PROGRESS.md — qué toca ahora
- CONVENTIONS.md — cómo se escribe código aquí
- docs/adr/ — decisiones congeladas

## Comandos
- Build: ./gradlew assembleDebug
- Tests: ./gradlew test
- Release: ./gradlew assembleRelease

## Reglas
1. Lee PROGRESS.md. Trabaja en la tarea DOING. Si no hay, pregunta cuál atacar.
2. Sigue CONVENTIONS.md.
3. Si introduces decisión nueva relevante → ADR. Si no es relevante, comentario en código.
4. Si hay ambigüedad en la tarea → pregunta. No asumas.
5. Al terminar: build verde + tests verdes + PROGRESS actualizado + commit limpio.
6. No toques docs/plan_tecnico.md ni ADRs existentes sin permiso explícito.
```

### 9.4 PROGRESS.md (estados simples)

```
# PROGRESS

## DOING
(una tarea o vacío)

## TODO
- T-002 — Project Android Studio + tema + navegación (4h)
- T-003 — Room base: entidades plantilla + DAOs + tests (5h)
- ...

## DONE
- T-001 — Setup repo + CLAUDE.md + 3 ADRs + CI (5h) · commit b1a2c3
```

Tres estados. Cuando una tarea está "esperando audit informal", sigue en DOING.

### 9.5 Formato de tarea

```
### T-NNN — Título
**Estimación**: Xh

**Descripción**
Qué hacer en 1-3 frases.

**Hecho cuando**
- Build verde
- Tú lo has probado en el móvil
- (lo que aplique para esta tarea)
```

Sin "criterios de aceptación" formales con checkbox por cada cosa. La definición de "hecho" la sabes mientras la construyes.

### 9.6 Definition of Done (3 pasos)

1. Build + tests verdes.
2. Lo pruebas tú en el móvil y funciona.
3. Commit convencional, PROGRESS actualizado.

Sin auditoría formal. Si algo te chirría, lo discutes con otra IA (o conmigo) pegando el código relevante. Sin plantilla.

### 9.7 Convenciones (ligero)

- Kotlin oficial. Sin detekt estricto al principio (Android Studio ya da warnings sanos). Si en algún momento el código deriva, añades detekt.
- Naming: dominio en español, infraestructura en inglés (`Medicion`, `EntradaComida`, `ComidaBaseRepository`).
- ViewModels: `StateFlow<UiState>` + `SharedFlow<Event>`.
- Tests AAA, nombres descriptivos.
- TODOs siempre con tarea o explicación.
- Funciones de ~30 líneas como guía, no regla.

### 9.8 Commits

Conventional Commits, ref a tarea opcional:

```
feat(nutricion): parser de comida con tool calling [T-023]
fix(entreno): RIR no puede ser negativo [T-031]
docs(adr): ADR-0003 Claude API + Sonnet
```

### 9.9 Riesgos de vibe coding

| Riesgo | Mitigación |
|---|---|
| Drift de estilo entre sesiones | CLAUDE.md + CONVENTIONS.md cortos pero claros |
| Pérdida de contexto al cambiar sesión | PROGRESS.md actualizado al inicio y fin de cada sesión |
| Tareas "completadas" que no funcionan | Pruebas tú en el móvil antes de cerrar |
| Cansarte del proceso | **El proceso está cortado al hueso. Si pesa, párate.** |

### 9.10 T-001 (volviendo a una sola tarea)

```
### T-001 — Setup del repo y archivos de proceso
**Estimación**: 5-7h

**Descripción**
Crear repo Git, project Android Studio mínimo (un Activity con Compose hola mundo),
9 archivos de proceso (§9.2), 3 ADRs fundacionales fundamentados, CI verde.

**Hecho cuando**
- Repo Git con primer push y CI verde
- 9 archivos de proceso existen con contenido real (no placeholders)
- 3 ADRs reflejan decisiones del plan v1.4
- CONVENTIONS.md cubre §9.7
- PROGRESS.md tiene al menos T-002 a T-006 redactadas
- Project compila y arranca con un Compose hola mundo
- Lo has probado en tu móvil
```

T-002 a T-006 redactadas dentro de T-001 son ~30 min de trabajo cada una, no horas. El backlog frío del resto lo construyes a medida que avanzas, no a priori.

---

## 10. PRINCIPIO RECTOR (recordatorio permanente)

Para Vic, y para cualquier Claude Code futuro que toque este código:

> **El mayor riesgo de este proyecto no es técnico. Es perder constancia, distraerse refinando, o empezar a "enterprise-izar" otra vez en mitad del camino.**

La tentación de añadir capas, patrones, abstracciones y procesos volverá cuando el código crezca. Cuando lo notes:

1. Pregunta primero "¿lo necesito ya, o estoy anticipando?".
2. Si la respuesta es "anticipando", no lo hagas. Apunta la idea, sigue.
3. Si añades algo nuevo "por si acaso", quítalo cuando confirmes que no hace falta.

Este plan está calibrado para una app personal seria. Si en algún momento sientes que el proyecto pesa más de lo que aporta, **párate antes de añadir cosas**. La acción correcta casi siempre es simplificar, no expandir.

---

*Plan v1.6 — 24/05/2026 — calibrado para constancia, no para perfección*
