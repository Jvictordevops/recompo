# CLAUDE.md

## Qué es esto
App Android personal de Vic. Tracking de recomposición corporal.
Kotlin + Compose + Room + Claude API. Un módulo, sin Hilt, local-only.

## Documentos
- `docs/plan_tecnico.md` — autoridad sobre QUÉ se construye
- `PROGRESS.md` — qué toca ahora
- `CONVENTIONS.md` — cómo se escribe código aquí
- `docs/adr/` — decisiones congeladas

## Comandos
- Build: `./gradlew assembleDebug`
- Tests: `./gradlew test`
- Release: `./gradlew assembleRelease`

## Reglas
1. Lee PROGRESS.md. Trabaja en la tarea DOING. Si no hay ninguna, pregunta cuál atacar.
2. Sigue CONVENTIONS.md.
3. Si introduces una decisión nueva relevante → ADR. Si no es relevante, comentario en código.
4. Si hay ambigüedad en la tarea → pregunta. No asumas.
5. Al terminar: build verde + tests verdes + PROGRESS actualizado + commit limpio.
6. No toques `docs/plan_tecnico.md` ni ADRs existentes sin permiso explícito de Vic.
