# ADR-0003 — Claude API como motor de IA

**Estado**: Aceptado  
**Fecha**: 2026-05-23

## Contexto

La app necesita tres capacidades de IA: parsear texto libre de comida a macros,
generar la próxima sesión de entrenamiento, y chat conversacional nutricional/entreno.

## Decisión

Claude API de Anthropic, modelo `claude-sonnet-4-6` (constante `ClaudeModels.DEFAULT_MODEL`).
API key en `local.properties` (gitignored), expuesta via `BuildConfig`.

**Tool calling** para los casos estructurados (parseo de comida, generación de sesión).
Schemas JSON en `assets/tools/`. **Chat** libre para las conversaciones.

**Límites de contexto** hardcoded desde el día 1 en `domain/ai/ContextLimits.kt`:
- Últimas 3 sesiones del mismo tipo
- Últimas 4 mediciones
- Últimos 20 mensajes en chat
- Máximo 8.000 caracteres en system + user prompt

## Alternativas descartadas

- **On-device (Gemini Nano / llama.cpp)**: capacidad insuficiente para razonamiento
  nutricional y generación de entreno. Sin tool calling fiable.
- **OpenAI GPT-4**: viable, pero Vic prefiere Claude. Cambiarlo es trivial si hace falta.

## Consecuencias

- Coste estimado: 5-10 €/mes en uso normal. Loggear `tokens_in/out` en `MensajeIA`.
- Si la API falla: fallbacks hardcoded (entrada manual, "repetir sesión +1 rep donde RIR≥3",
  mensaje de error en chat).
- La API key en el APK es aceptable: distribución privada, APK solo en el móvil de Vic.
- Riesgo de alucinaciones en macros: validación de plausibilidad + confianza visible + editable.
