package com.vic.recompo.domain.usecase

import com.vic.recompo.data.ai.ClaudeApi
import com.vic.recompo.data.ai.dto.ClaudeRequest
import com.vic.recompo.data.ai.dto.Message
import retrofit2.HttpException

class ChatUseCase(private val claudeApi: ClaudeApi) {

    sealed class Resultado {
        data class Exito(val texto: String, val tokensIn: Int, val tokensOut: Int) : Resultado()
        data class Fallo(val mensaje: String) : Resultado()
    }

    suspend fun enviar(systemPrompt: String, mensajes: List<Message>): Resultado {
        return try {
            val response = claudeApi.messages(
                ClaudeRequest(
                    maxTokens = 1024,
                    system = systemPrompt,
                    messages = mensajes
                )
            )
            val texto = response.content.firstOrNull { it.type == "text" }?.text
                ?: return Resultado.Fallo("Respuesta inesperada de la IA")
            Resultado.Exito(texto, response.usage.inputTokens, response.usage.outputTokens)
        } catch (e: HttpException) {
            Resultado.Fallo("Error de red (HTTP ${e.code()})")
        } catch (e: Exception) {
            Resultado.Fallo("Error: ${e.message}")
        }
    }
}
