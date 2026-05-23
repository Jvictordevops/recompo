# ADR-0001 — Stack técnico

**Estado**: Aceptado  
**Fecha**: 2026-05-23

## Contexto

App personal de tracking de recomposición corporal para un único usuario (Vic).
Necesita funcionar offline, ser mantenible a largo plazo con sesiones de vibe coding,
y tener fricción mínima de distribución (no Play Store).

## Decisión

| Capa | Elección |
|---|---|
| Plataforma | Android nativo |
| Lenguaje / UI | Kotlin + Jetpack Compose |
| Persistencia | Room (SQLite local) |
| Inyección dependencias | Constructor injection manual + ViewModelFactory |
| Motor de IA | Claude API — `claude-sonnet-4-6` |
| Distribución | APK firmado, sideload |
| Estructura | Un único módulo Gradle `:app` |

## Consecuencias

- Sin Hilt: los ViewModels se instancian a mano en `App.kt`. Más simple, compila más rápido,
  menos magia. El coste es repetición mínima en las factories (~6-8 ViewModels).
- Sin multi-módulo: toda la lógica en paquetes dentro de `:app`. Suficiente para una app
  de esta escala.
- Compose: curva de aprendizaje inicial, pero es el estándar moderno de Android UI.
  Empezar por pantallas simples (mediciones, actividad).
- APK sideload: sin proceso de release en Play Store. Distribución en < 1 minuto.
