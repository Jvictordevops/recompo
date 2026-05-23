# CONVENTIONS.md

## Lenguaje y naming
- Dominio en español: `Medicion`, `EntradaComida`, `ComidaBase`, `Sesion`, `Actividad`
- Infraestructura en inglés: `ComidaBaseRepository`, `MedicionDao`, `ClaudeApi`
- Enums en español mayúsculas: `SlotComida.DESAYUNO`, `EstadoSesion.COMPLETADA`

## Arquitectura
- MVVM: ViewModel + `StateFlow<UiState>` + `SharedFlow<Event>` para acciones one-shot
- Repositorios: clases normales, instanciadas en `App.kt`, pasadas via `ViewModelFactory`
- Sin Hilt. Constructor injection manual.

## Compose
- Pantallas = `@Composable fun XxxScreen(viewModel: XxxViewModel)`
- Componentes reutilizables en `ui/common/`
- Sin lógica de negocio en Composables

## Room
- IDs: `Long` autogenerado (`@PrimaryKey(autoGenerate = true)`)
- Fechas: `LocalDate` para días, `Instant` para timestamps — type converters obligatorios
- Enums: type converter a `String`, nunca ordinal
- `fallbackToDestructiveMigration()` hasta tener 2-3 semanas de datos reales

## Código
- Funciones ~30 líneas como guía, no regla
- Sin comentarios obvios; comentar solo el WHY no obvio
- TODOs siempre con referencia a tarea: `// TODO T-023: parsear confianza baja`
- Sin abstracciones anticipadas — si no lo necesitas ya, no lo hagas

## Tests
- Nombres descriptivos: `calcula_grasa_navy_con_mediciones_validas()`
- AAA (Arrange / Act / Assert), sin comentarios de sección
- Solo donde aportan: cálculos domain, DAOs críticos, parser respuesta IA

## Commits
Conventional Commits, referencia a tarea opcional:
```
feat(nutricion): parser de comida con tool calling [T-023]
fix(entreno): RIR no puede ser negativo [T-031]
docs(adr): ADR-0003 Claude API + Sonnet
```
