package com.vic.recompo.data.ai

import com.vic.recompo.data.ai.dto.ClaudeRequest
import com.vic.recompo.domain.ai.ParseComidaResult
import com.vic.recompo.data.ai.dto.ClaudeResponse
import com.vic.recompo.data.ai.dto.ContentBlock
import com.vic.recompo.data.ai.dto.Usage
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParseComidaUseCaseTest {

    private val schema = """
        {
          "type": "object",
          "properties": {
            "kcal": { "type": "integer" },
            "proteina_g": { "type": "number" },
            "grasa_g": { "type": "number" },
            "carbo_g": { "type": "number" },
            "confianza": { "type": "string", "enum": ["alta", "media", "baja"] },
            "notas": { "type": "string" }
          },
          "required": ["kcal", "proteina_g", "grasa_g", "carbo_g", "confianza"]
        }
    """.trimIndent()

    private fun fakeApi(response: ClaudeResponse) = object : ClaudeApi {
        override suspend fun messages(request: ClaudeRequest) = response
    }

    private fun fakeResponse(input: kotlinx.serialization.json.JsonObject) = ClaudeResponse(
        id = "test",
        type = "message",
        role = "assistant",
        content = listOf(
            ContentBlock(type = "tool_use", name = "registrar_comida", input = input)
        ),
        model = "claude-sonnet-4-6",
        stopReason = "tool_use",
        usage = Usage(inputTokens = 100, outputTokens = 50)
    )

    @Test
    fun parsea_comida_valida_devuelve_result_success() = runTest {
        val input = buildJsonObject {
            put("kcal", 374)
            put("proteina_g", 38.0)
            put("grasa_g", 8.0)
            put("carbo_g", 38.0)
            put("confianza", "alta")
        }
        val useCase = ParseComidaUseCase(fakeApi(fakeResponse(input)), schema)

        val result = useCase.parsear("leche con avena y whey")

        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow() as ParseComidaResult.Calculado
        assertEquals(374, parsed.kcal)
        assertEquals(38.0, parsed.proteinaG, 0.01)
        assertEquals(8.0, parsed.grasaG, 0.01)
        assertEquals(38.0, parsed.carboG, 0.01)
        assertEquals("alta", parsed.confianza)
    }

    @Test
    fun sin_tool_use_en_respuesta_devuelve_result_failure() = runTest {
        val response = ClaudeResponse(
            id = "test",
            type = "message",
            role = "assistant",
            content = listOf(ContentBlock(type = "text", text = "No puedo calcular eso")),
            model = "claude-sonnet-4-6",
            stopReason = "end_turn",
            usage = Usage(inputTokens = 50, outputTokens = 20)
        )
        val useCase = ParseComidaUseCase(fakeApi(response), schema)

        val result = useCase.parsear("algo")

        assertTrue(result.isFailure)
    }

    @Test
    fun api_lanza_excepcion_devuelve_result_failure() = runTest {
        val errorApi = object : ClaudeApi {
            override suspend fun messages(request: ClaudeRequest): ClaudeResponse =
                throw RuntimeException("timeout")
        }
        val useCase = ParseComidaUseCase(errorApi, schema)

        val result = useCase.parsear("algo")

        assertTrue(result.isFailure)
    }
}
