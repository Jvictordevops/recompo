# ADR-0004 — Cálculo dinámico de kcalObjetivo basado en actividad real

**Estado**: Aceptado
**Fecha**: 2026-05-24

## Contexto

El plan v1.5 §2.1 definía tres campos en `UserSettings` para representar el kcal
objetivo según el tipo de día: `kcalDescanso`, `kcalMusculacion`, `kcalBici`.
`TipoDia` se derivaba en `HomeViewModel` (T-005) y seleccionaba uno de los tres
como `kcalObjetivo` del día.

Este modelo tiene dos problemas:

1. **Rigidez**: la cantidad de kcal quemadas en una sesión de bici varía mucho de
   un día a otro (45 min de paseo ≠ 90 min de salida). Un valor fijo `kcalBici`
   no refleja la realidad.
2. **Agujero de coherencia en el plan**: §2.5 introduce `Actividad.kcalQuemadas`
   como input manual obligatorio, pero el plan nunca define la fórmula que conecta
   esos kcal con el objetivo diario. La intención original del usuario (registrada
   en iteraciones previas de diseño) era que la actividad real ajustase el objetivo
   del día.

## Decisión

Sustituir los tres campos por un único `kcalBaseDia: Int` en `UserSettings`. El
`kcalObjetivo` del día se calcula en runtime:

```
kcalObjetivo = kcalBaseDia + sum(actividadesHoy.kcalQuemadas)
```

Implementación: `domain/usecase/CalcularKcalObjetivoUseCase.kt`. `HomeViewModel`
combina el flow de actividades del día con `UserSettings.kcalBaseDia`.

`TipoDia` (MUSCULACION / BICI / DESCANSO) se mantiene como **etiqueta visual
informativa** en Home (chip de tipo de día), pero **no afecta al cálculo** del
objetivo.

## Alternativas descartadas

- **Sumar actividad sobre `kcalMusculacion` / `kcalBici`** (modelo "B literal" en
  la discusión de diseño): produce doble conteo, porque esos valores ya incluían
  un margen genérico por entrenar/pedalear. El modelo único `kcalBaseDia` evita el
  problema y es más predecible.
- **Tabla `Fase` con histórico de macros por fase**: sobreingeniería para una sola
  persona con una fase activa a la vez (§2.1). Si llega el caso de necesitar
  histórico, se modela entonces.

## Consecuencias

- **Cambio de schema en `UserSettings`**: T-004 ya está hecha, hay que refactorizar.
  Como no hay datos reales todavía en producción, borrar la app + reconfigurar
  wizard vale como "migración". No se implementa lectura del schema viejo.
- **Cambio en wizard (T-004)**: paso 2 captura un solo valor de kcal (default
  sugerido 1900) en lugar de tres.
- **Cambio en `HomeViewModel` (T-005)**: pasa de lectura directa a combinar
  `UserSettings.kcalBaseDia` con el flow de actividades del día vía use case.
- **Cambio en Settings (T-011)**: el editor de macros sólo expone `kcalBaseDia` y
  `proteinaObjetivoG`.
- **Disciplina del usuario**: si Vic se olvida de registrar una actividad, el día
  queda con sólo `kcalBaseDia` (déficit estricto). Es consciente y forma parte de
  la disciplina del plan — la app no inventa actividad.

## Tareas afectadas

- **T-008** (Actividad + refactor a kcalObjetivo dinámico): aplica los cambios de
  modelo (fase A) y añade el CRUD de actividad (fase B).
- **T-011** (Settings básico): el editor de macros se ajusta al nuevo modelo.
