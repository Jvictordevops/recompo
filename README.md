# Recompo

App Android personal de tracking de recomposición corporal.

**Stack**: Kotlin + Jetpack Compose + Room + Claude API (Sonnet 4.6)  
**Plataforma**: Android, APK sideload  
**Distribución**: privada, uso personal

## Documentación

- `docs/plan_tecnico.md` — plan técnico completo
- `PROGRESS.md` — backlog y estado actual
- `CLAUDE.md` — contexto para Claude Code
- `CONVENTIONS.md` — convenciones de código
- `docs/adr/` — decisiones de arquitectura

## Desarrollo

```bash
# Build debug
./gradlew assembleDebug

# Tests
./gradlew test

# Build release (requiere keystore)
./gradlew assembleRelease
```

Requiere `local.properties` con `CLAUDE_API_KEY`. Ver `local.properties.example`.
