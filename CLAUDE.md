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
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug    # build debug
./gradlew test             # tests
./gradlew assembleRelease  # release (requiere keystore)
```

El JAVA_HOME está en ~/.bash_profile, pero si falla en terminal nueva, exportarlo manualmente.

## Git — cuenta personal
Los pushes se hacen con el script `~/proyectos/git-personal`:
```bash
~/proyectos/git-personal push origin main
```
El remote usa HTTPS: `https://github.com/jvictordevops/recompo.git`

## Decisiones técnicas (no obvias)
- **kotlin.android plugin**: NO se aplica explícitamente. AGP 9.x lo incluye implícito. Añadirlo causa "extension already registered" error.
- **KSP**: versión `2.3.8` (nuevo esquema sin prefijo Kotlin desde KSP 2.3.x). No usar el formato antiguo `{kotlin}-{ksp}`.
- **kotlinOptions**: deprecado en AGP 9.x. El JVM target se configura solo con `compileOptions { sourceCompatibility/targetCompatibility = VERSION_17 }`.
- **material-icons-extended**: dependencia separada del BOM. Necesaria para iconos como FitnessCenter, Restaurant, ShowChart.
- **compileSdk**: 35 (no 36 — el formato `release(36)` que genera Android Studio con AGP 9.2.1 da problemas, usar Int plano).

## Reglas
1. Lee PROGRESS.md. Trabaja en la tarea DOING. Si no hay ninguna, pregunta cuál atacar.
2. Sigue CONVENTIONS.md.
3. Si introduces una decisión nueva relevante → apúntala en este fichero o en un ADR.
4. Si hay ambigüedad en la tarea → pregunta. No asumas.
5. Al terminar: build verde + tests verdes + PROGRESS actualizado + commit limpio.
6. No toques `docs/plan_tecnico.md` ni ADRs existentes sin permiso explícito de Vic.
