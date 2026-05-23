# ADR-0002 — Local-only, sin backend, sin auth

**Estado**: Aceptado  
**Fecha**: 2026-05-23

## Contexto

La app es para uso personal exclusivo de Vic. No hay necesidad de sincronización
entre dispositivos, ni de auth, ni de backend propio.

## Decisión

Todos los datos viven en SQLite local (Room). No hay servidor, no hay cuenta de usuario,
no hay sync en tiempo real entre dispositivos.

**Backup**: JSON continuo a Google Drive vía SAF (Storage Access Framework).
El archivo se escribe localmente en la carpeta Drive elegida por el usuario.
Export XLSX bajo demanda. Export ZIP completo como copia de seguridad manual.

## Alternativas descartadas

- **Firebase / Supabase**: overhead de auth, latencia, coste, complejidad. No aporta
  valor para un único usuario.
- **Sync entre dispositivos**: descartado. Si llega el caso, migrar IDs a UUID.
- **Health Connect / Strava**: descartado. El modelo `Actividad` lo soportaría si llegara.

## Consecuencias

- La app funciona sin conexión a internet (excepto Claude API).
- Pérdida del móvil = pérdida de datos si el backup a Drive falla.
  Mitigación: `BackupChip` visible en Home + alerta si >48h sin backup OK.
- Sin conflictos de merge ni problemas de concurrencia.
