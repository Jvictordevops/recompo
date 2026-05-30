package com.vic.recompo.domain.usecase

import android.content.Context
import com.vic.recompo.data.ai.ClaudeApi
import com.vic.recompo.data.ai.dto.ClaudeRequest
import com.vic.recompo.data.ai.dto.Message
import com.vic.recompo.data.ai.dto.Tool
import com.vic.recompo.domain.ai.ContextLimits
import com.vic.recompo.domain.ai.DeloadCalendar
import com.vic.recompo.domain.model.EstadoSerie
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import retrofit2.HttpException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GenerarSesionUseCase(
    private val claudeApi: ClaudeApi,
    private val schemaJson: String,
    private val context: Context
) {

    sealed class Resultado {
        data class Exito(val propuesta: ProximaSesionPropuesta) : Resultado()
        data class Fallo(val mensaje: String, val fallback: ProximaSesionPropuesta?) : Resultado()
    }

    data class ContextoSinHistorico(
        val equipamiento: String,
        val tiempoMinutos: Int?
    )

    private val json = Json { ignoreUnknownKeys = true }

    private val tool by lazy {
        Tool(
            name = TOOL_NAME,
            description = "Propone los ejercicios, series, reps y cargas de la próxima sesión de entrenamiento",
            inputSchema = json.parseToJsonElement(schemaJson).jsonObject
        )
    }

    private val toolChoice by lazy {
        buildJsonObject {
            put("type", "tool")
            put("name", TOOL_NAME)
        }
    }

    suspend fun generar(
        tipoNombre: String,
        historial: List<SesionConDetalle>,
        contextoSinHistorico: ContextoSinHistorico? = null
    ): Resultado {
        val systemPrompt = construirSystemPrompt(tipoNombre, historial, contextoSinHistorico)
        val userMsg = if (historial.isEmpty()) {
            val equip = contextoSinHistorico?.equipamiento ?: "GYM"
            val tiempo = contextoSinHistorico?.tiempoMinutos?.let { " Tiempo disponible: ${it} min." } ?: ""
            "Genera una sesión $tipoNombre desde cero. Equipamiento: $equip.$tiempo"
        } else {
            "Genera la próxima sesión $tipoNombre siguiendo las reglas de progresión del sistema."
        }

        return try {
            val request = ClaudeRequest(
                maxTokens = 1024,
                system = systemPrompt,
                messages = listOf(Message(role = "user", content = userMsg)),
                tools = listOf(tool),
                toolChoice = toolChoice
            )
            val response = claudeApi.messages(request)
            val toolUse = response.content.firstOrNull { it.type == "tool_use" && it.name == TOOL_NAME }
                ?: return Resultado.Fallo("Respuesta inesperada de la IA", generarFallback(historial))

            val input = toolUse.input
                ?: return Resultado.Fallo("Respuesta vacía de la IA", generarFallback(historial))

            val razonamiento = input["razonamiento"]?.jsonPrimitive?.content ?: ""
            val ejerciciosJson = input["ejercicios"]?.jsonArray ?: return Resultado.Fallo("Sin ejercicios en la respuesta", generarFallback(historial))

            val ejercicios = ejerciciosJson.map { ej ->
                val obj = ej.jsonObject
                EjercicioPropuesto(
                    nombreEjercicio = obj["nombreEjercicio"]?.jsonPrimitive?.content ?: "",
                    seriesObjetivo = obj["seriesObjetivo"]?.jsonPrimitive?.intOrNull ?: 3,
                    repsMin = obj["repsMin"]?.jsonPrimitive?.intOrNull ?: 8,
                    repsMax = obj["repsMax"]?.jsonPrimitive?.intOrNull ?: 12,
                    cargaObjetivoKg = obj["cargaObjetivoKg"]?.jsonPrimitive?.doubleOrNull,
                    notas = obj["notas"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
                )
            }.filter { it.nombreEjercicio.isNotBlank() }

            if (ejercicios.isEmpty()) Resultado.Fallo("La IA no propuso ejercicios", generarFallback(historial))
            else Resultado.Exito(ProximaSesionPropuesta(razonamiento, ejercicios))

        } catch (e: HttpException) {
            val errBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull() ?: ""
            Resultado.Fallo("Error de red (HTTP ${e.code()}): $errBody", generarFallback(historial))
        } catch (e: Exception) {
            Resultado.Fallo("Error: ${e.message}", generarFallback(historial))
        }
    }

    private fun construirSystemPrompt(
        tipoNombre: String,
        historial: List<SesionConDetalle>,
        contextoSinHistorico: ContextoSinHistorico?
    ): String {
        val plantilla = context.assets.open("prompts/system_sesion.txt").bufferedReader().use { it.readText() }
        val equipamiento = contextoSinHistorico?.equipamiento ?: "GYM"
        val deloadCtx = DeloadCalendar.deloadContextText(LocalDate.now())
        val historialTexto = if (historial.isEmpty()) {
            "Sin historial previo para este tipo de sesión. Genera desde cero."
        } else {
            historial.joinToString("\n\n") { sesionConDetalle ->
                formatearSesion(sesionConDetalle)
            }
        }
        return plantilla
            .replace("{EQUIPAMIENTO}", equipamiento)
            .replace("{FECHA_HOY}", LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
            .replace("{DELOAD_CONTEXTO}", deloadCtx)
            .replace("{TIPO_SESION}", tipoNombre)
            .replace("{HISTORIAL_SESIONES}", historialTexto)
    }

    private fun formatearSesion(sc: SesionConDetalle): String {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val fechaStr = sc.sesion.fechaEjecutada?.let {
            java.time.Instant.ofEpochMilli(it.toEpochMilli())
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(fmt)
        } ?: "sin fecha"
        val rirGlobalStr = sc.sesion.rirGlobal?.let { " · RIR global: $it" } ?: ""
        val notasStr = sc.sesion.notasGlobales?.let { " · notas: \"$it\"" } ?: ""
        val header = "=== Sesión ${sc.tipoNombre} ($fechaStr)$rirGlobalStr$notasStr ==="
        val ejercicios = sc.ejerciciosConSeries.mapIndexed { idx, ec ->
            val ees = ec.ejercicioEnSesion
            val carga = ees.cargaObjetivoKg?.let { " @ ${it}kg" } ?: ""
            val objetivo = "${ees.seriesObjetivo}×${ees.repsObjetivoMin}-${ees.repsObjetivoMax}$carga"
            val seriesReales = ec.series.joinToString(", ") { serie ->
                when (serie.estado) {
                    EstadoSerie.OMITIDA -> "S${serie.numero}: OMITIDA${serie.motivoOmision?.let { " [${it.name}]" } ?: ""}"
                    EstadoSerie.COMPLETADA -> "S${serie.numero}: ${serie.repsReales}r·${serie.cargaKg}kg"
                }
            }
            val rirEj = ees.rir?.let { " (RIR: $it)" } ?: ""
            val notas = ees.notas?.let { " | nota: $it" } ?: ""
            "${idx + 1}. ${ec.ejercicio.nombre}  obj: $objetivo  |  reales: $seriesReales$rirEj$notas"
        }.joinToString("\n")
        return "$header\n$ejercicios"
    }

    fun generarFallback(historial: List<SesionConDetalle>): ProximaSesionPropuesta? {
        val ultima = historial.firstOrNull() ?: return null
        val ejercicios = ultima.ejerciciosConSeries.map { ec ->
            val ees = ec.ejercicioEnSesion
            val rir = ees.rir ?: 3
            val progresa = rir >= 3
            EjercicioPropuesto(
                nombreEjercicio = ec.ejercicio.nombre,
                seriesObjetivo = ees.seriesObjetivo,
                repsMin = if (progresa) ees.repsObjetivoMin + 1 else ees.repsObjetivoMin,
                repsMax = if (progresa) ees.repsObjetivoMax + 1 else ees.repsObjetivoMax,
                cargaObjetivoKg = ees.cargaObjetivoKg,
                notas = if (progresa) "+1 rep (RIR≥3)" else null
            )
        }
        return ProximaSesionPropuesta(
            razonamiento = "Sin conexión con la IA. Repetición de la última sesión con ajuste mínimo: +1 rep donde RIR≥3.",
            ejercicios = ejercicios
        )
    }

    companion object {
        private const val TOOL_NAME = "proponer_sesion"
        const val MAX_HISTORIAL = ContextLimits.MAX_SESIONES_HISTORICO
    }
}
