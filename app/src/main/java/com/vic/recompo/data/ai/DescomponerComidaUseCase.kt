package com.vic.recompo.data.ai

import com.vic.recompo.data.ai.dto.ClaudeRequest
import com.vic.recompo.data.ai.dto.Message
import com.vic.recompo.data.ai.dto.Tool
import com.vic.recompo.data.db.dao.UsoIADao
import com.vic.recompo.data.db.entity.UsoIA
import com.vic.recompo.domain.ai.IngredienteDescompuesto
import com.vic.recompo.domain.ai.TarifasIA
import com.vic.recompo.domain.model.FuncionIA
import com.vic.recompo.domain.model.ProveedorIA
import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import retrofit2.HttpException
import timber.log.Timber

class DescomponerComidaUseCase(
    private val claudeApi: ClaudeApi,
    private val schemaJson: String,
    private val usoIADao: UsoIADao
) {

    private val json = Json { ignoreUnknownKeys = true }

    private val tool by lazy {
        Tool(
            name = TOOL_NAME,
            description = "Descompone una comida en su lista de ingredientes con cantidades y valores nutricionales por 100g",
            inputSchema = json.parseToJsonElement(schemaJson).jsonObject
        )
    }

    private val toolChoice by lazy {
        buildJsonObject {
            put("type", "tool")
            put("name", TOOL_NAME)
        }
    }

    suspend fun descomponer(textoComida: String): Result<List<IngredienteDescompuesto>> {
        return try {
            val request = ClaudeRequest(
                maxTokens = 1024,
                system = SYSTEM_PROMPT,
                messages = listOf(Message(role = "user", content = textoComida)),
                tools = listOf(tool),
                toolChoice = toolChoice
            )
            val response = claudeApi.messages(request)
            usoIADao.insert(UsoIA(
                timestamp = Instant.now(),
                funcion = FuncionIA.DESCOMPOSICION_COMIDA,
                proveedor = ProveedorIA.CLAUDE_NATIVO,
                modelo = ClaudeModels.DEFAULT_MODEL,
                tokensIn = response.usage.inputTokens,
                tokensOut = response.usage.outputTokens,
                costeUsd = TarifasIA.calcularCosteUsd(ClaudeModels.DEFAULT_MODEL, response.usage.inputTokens, response.usage.outputTokens)
            ))
            val toolUse = response.content.firstOrNull { it.type == "tool_use" && it.name == TOOL_NAME }
                ?: return Result.failure(IllegalStateException("Sin tool_use en respuesta"))
            val input = toolUse.input
                ?: return Result.failure(IllegalStateException("input nulo en tool_use"))

            val ingredientes = input["ingredientes"]?.jsonArray?.mapNotNull { elem ->
                val obj = runCatching { elem.jsonObject }.getOrNull() ?: return@mapNotNull null
                val nombre = obj["nombre"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val cantidadG = obj["cantidad_g"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                val kcal100g = obj["kcal_100g"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                val prot100g = obj["prot_100g"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                val grasa100g = obj["grasa_100g"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                val carbo100g = obj["carbo_100g"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
                IngredienteDescompuesto(nombre, cantidadG, kcal100g, prot100g, grasa100g, carbo100g)
            } ?: emptyList()

            if (ingredientes.isEmpty()) {
                Result.failure(IllegalStateException("La IA no devolvió ingredientes"))
            } else {
                Result.success(ingredientes)
            }
        } catch (e: HttpException) {
            val errBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull() ?: "sin body"
            Timber.w("DescomponerComida HTTP ${e.code()}: $errBody")
            Result.failure(RuntimeException("HTTP ${e.code()}: $errBody", e))
        } catch (e: Exception) {
            Timber.w(e, "DescomponerComida failed")
            Result.failure(e)
        }
    }

    companion object {
        private const val TOOL_NAME = "descomponer_comida"
        private const val SYSTEM_PROMPT = """Eres un nutricionista experto. Descompón la comida descrita en sus ingredientes individuales con las cantidades típicas de una porción.

Para cada ingrediente:
- Usa el nombre más descriptivo y específico en español ("Pechuga de pollo" no "pollo", "Aceite de oliva virgen extra" no "aceite")
- Indica la cantidad en gramos para esta preparación concreta
- Proporciona valores nutricionales por 100g según USDA/BEDCA

Porciones estándar si no se especifica cantidad:
- Pasta/arroz: 80g en seco
- Carne o pescado en plato principal: 150g
- Pan bocadillo: 80g / sándwich (2 rebanadas): 60g
- Aceite por cucharada: 10g
- Huevo: 60g

Responde siempre usando la herramienta descomponer_comida."""
    }
}
