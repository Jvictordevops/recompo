package com.vic.recompo.data.ai

import com.vic.recompo.data.ai.dto.ClaudeRequest
import com.vic.recompo.data.ai.dto.Message
import com.vic.recompo.data.ai.dto.Tool
import com.vic.recompo.domain.ai.ParseComidaResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import retrofit2.HttpException
import timber.log.Timber

class ParseComidaUseCase(
    private val claudeApi: ClaudeApi,
    private val schemaJson: String
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val tool by lazy {
        Tool(
            name = TOOL_NAME,
            description = "Registra el análisis nutricional de una comida",
            inputSchema = json.parseToJsonElement(schemaJson).jsonObject
        )
    }

    private val toolChoice by lazy {
        buildJsonObject {
            put("type", "tool")
            put("name", TOOL_NAME)
        }
    }

    suspend fun parsear(textoLibre: String): Result<ParseComidaResult> =
        llamar(listOf(Message(role = "user", content = textoLibre)))

    suspend fun parsearConContexto(
        textoOriginal: String,
        contextoAdicional: String,
        forzarCalculo: Boolean = false
    ): Result<ParseComidaResult> {
        val contenido = buildString {
            append("$textoOriginal\nContexto adicional: $contextoAdicional")
            if (forzarCalculo) append("\n\nCalcula los macros ahora con la información disponible. NO hagas más preguntas; si falta algún dato, estima con porciones estándar y marca confianza=\"baja\".")
        }
        return llamar(listOf(Message(role = "user", content = contenido)))
    }

    private suspend fun llamar(messages: List<Message>): Result<ParseComidaResult> {
        return try {
            val request = ClaudeRequest(
                maxTokens = 512,
                system = SYSTEM_PROMPT,
                messages = messages,
                tools = listOf(tool),
                toolChoice = toolChoice
            )
            val response = claudeApi.messages(request)
            val toolUse = response.content.firstOrNull { it.type == "tool_use" && it.name == TOOL_NAME }
                ?: return Result.failure(IllegalStateException("Sin tool_use en respuesta"))

            val input = toolUse.input
                ?: return Result.failure(IllegalStateException("input nulo en tool_use"))

            Result.success(parseInput(input))
        } catch (e: HttpException) {
            val errBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull() ?: "sin body"
            Timber.w("ParseComida HTTP ${e.code()}: $errBody")
            Result.failure(RuntimeException("HTTP ${e.code()}: $errBody", e))
        } catch (e: Exception) {
            Timber.w(e, "ParseComida failed")
            Result.failure(e)
        }
    }

    private fun parseInput(input: JsonObject): ParseComidaResult {
        val necesita = input["necesita_pregunta"]?.jsonPrimitive?.booleanOrNull ?: false
        return if (necesita) {
            val preguntas = runCatching {
                input["preguntas"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            }.getOrDefault(emptyList())
            ParseComidaResult.NecesitaAclaracion(preguntas)
        } else {
            val kcal = input["kcal"]?.jsonPrimitive?.intOrNull
                ?: throw IllegalStateException("kcal inválido")
            val proteinaG = input["proteina_g"]?.jsonPrimitive?.doubleOrNull
                ?: throw IllegalStateException("proteina_g inválido")
            val grasaG = input["grasa_g"]?.jsonPrimitive?.doubleOrNull
                ?: throw IllegalStateException("grasa_g inválido")
            val carboG = input["carbo_g"]?.jsonPrimitive?.doubleOrNull
                ?: throw IllegalStateException("carbo_g inválido")
            val confianza = input["confianza"]?.jsonPrimitive?.contentOrNull ?: "media"
            val supuestos = input["supuestos"]?.jsonPrimitive?.contentOrNull
            ParseComidaResult.Calculado(kcal, proteinaG, grasaG, carboG, confianza, supuestos)
        }
    }

    companion object {
        private const val TOOL_NAME = "registrar_comida"
        private const val SYSTEM_PROMPT = """Eres un asistente experto en nutrición deportiva.
Antes de calcular, evalúa si la descripción tiene ambigüedad RELEVANTE, definida como:
- cambia las kcal estimadas en más del 15%, O
- cambia la proteína en más de 5g.

Si la ambigüedad es RELEVANTE: devuelve necesita_pregunta=true con 1-2 preguntas mínimas imprescindibles.
Ejemplos de ambigüedad relevante: atún (al natural / en aceite / escurrido), pasta o arroz (peso en seco vs cocido), carne (corte con o sin piel), cantidades vagas ("un plato", "un puñado").

Si la ambigüedad es DESPRECIABLE (tomate al gusto, sal, especias, limón): NO preguntes. Asume el caso más común y decláralo en supuestos.

LÍMITE DE PREGUNTAS: puedes preguntar como MÁXIMO una vez por entrada de comida.
Tras la respuesta del usuario (cualquiera que sea), DEBES calcular los macros. No vuelvas a preguntar.
Si el usuario responde que no sabe la cantidad ("no lo sé", "ni idea", "estima tú", "lo normal", o similar), o si su respuesta no resuelve la duda: estima usando porciones estándar y marca confianza="baja". Declara la estimación en supuestos.
Preguntar sirve para mejorar la estimación, NO es requisito para calcular. Ante falta de datos, SIEMPRE puedes estimar. Nunca bloquees el registro por falta de una cantidad.

PORCIONES ESTÁNDAR (usar cuando no se indique cantidad):
- Loncha de salmón ahumado: 30g
- Rebanada pan de molde: 30g / tostada rústica: 40g
- Loncha de pavo, jamón cocido, lomo: 20-25g
- Cucharada de aceite: 10ml
- Puñado de frutos secos: 25g
- Pieza de fruta media (manzana, plátano): 120-150g
- Ración de proteína principal (carne/pescado) en plato: 120-150g si no se especifica

Cuando calcules (necesita_pregunta=false): rellena siempre supuestos con lo que has asumido, en lenguaje claro y breve.
Ejemplo: "Atún al natural escurrido. Tomate ~50g. Sin aceite añadido."

Estima conservador: ante duda dentro de un rango, tira a kcal alta y proteína baja.
Usa valores estándar de bases de datos nutricionales (USDA, BEDCA).
Responde siempre usando la herramienta registrar_comida."""
    }
}
