package com.vic.recompo.data.ai

import com.vic.recompo.data.ai.dto.ClaudeRequest
import com.vic.recompo.data.ai.dto.Message
import com.vic.recompo.data.ai.dto.Tool
import com.vic.recompo.domain.ai.ParseComidaResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
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
            description = "Registra los macros nutricionales calculados de una comida",
            inputSchema = json.parseToJsonElement(schemaJson).jsonObject
        )
    }

    private val toolChoice by lazy {
        buildJsonObject {
            put("type", "tool")
            put("name", TOOL_NAME)
        }
    }

    suspend fun parsear(textoLibre: String): Result<ParseComidaResult> {
        return try {
            val request = ClaudeRequest(
                maxTokens = 512,
                system = SYSTEM_PROMPT,
                messages = listOf(Message(role = "user", content = textoLibre)),
                tools = listOf(tool),
                toolChoice = toolChoice
            )
            val response = claudeApi.messages(request)
            val toolUse = response.content.firstOrNull { it.type == "tool_use" && it.name == TOOL_NAME }
                ?: return Result.failure(IllegalStateException("Sin tool_use en respuesta"))

            val input = toolUse.input
                ?: return Result.failure(IllegalStateException("input nulo en tool_use"))

            val kcal = input["kcal"]?.jsonPrimitive?.intOrNull
                ?: return Result.failure(IllegalStateException("kcal inválido"))
            val proteinaG = input["proteina_g"]?.jsonPrimitive?.doubleOrNull
                ?: return Result.failure(IllegalStateException("proteina_g inválido"))
            val grasaG = input["grasa_g"]?.jsonPrimitive?.doubleOrNull
                ?: return Result.failure(IllegalStateException("grasa_g inválido"))
            val carboG = input["carbo_g"]?.jsonPrimitive?.doubleOrNull
                ?: return Result.failure(IllegalStateException("carbo_g inválido"))
            val confianza = input["confianza"]?.jsonPrimitive?.contentOrNull ?: "media"
            val notas = input["notas"]?.jsonPrimitive?.contentOrNull

            Timber.d("ParseComida OK: %d kcal, confianza=%s, tokens=%d", kcal, confianza, response.usage.inputTokens + response.usage.outputTokens)
            Result.success(ParseComidaResult(kcal, proteinaG, grasaG, carboG, confianza, notas))
        } catch (e: HttpException) {
            val errBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull() ?: "sin body"
            android.util.Log.e("ParseComida", "HTTP ${e.code()}: $errBody")
            Result.failure(RuntimeException("HTTP ${e.code()}: $errBody", e))
        } catch (e: Exception) {
            Timber.w(e, "ParseComida failed")
            android.util.Log.e("ParseComida", "parsear failed: ${e::class.simpleName}: ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TOOL_NAME = "registrar_comida"
        private const val SYSTEM_PROMPT = """Eres un asistente experto en nutrición deportiva.
Cuando el usuario describa una comida, calcula sus macros nutricionales usando la herramienta registrar_comida.
Usa valores estándar de bases de datos nutricionales (USDA, BEDCA).
Si no se especifica gramaje, usa 100g como referencia e indica confianza "baja".
Responde siempre usando la herramienta."""
    }
}
