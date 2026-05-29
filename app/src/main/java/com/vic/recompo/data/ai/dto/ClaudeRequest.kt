package com.vic.recompo.data.ai.dto

import com.vic.recompo.data.ai.ClaudeModels
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ClaudeRequest(
    val model: String = ClaudeModels.DEFAULT_MODEL,
    @SerialName("max_tokens") val maxTokens: Int = 4096,
    val system: String? = null,
    val messages: List<Message>,
    val tools: List<Tool>? = null
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class Tool(
    val name: String,
    val description: String,
    @SerialName("input_schema") val inputSchema: JsonObject
)
