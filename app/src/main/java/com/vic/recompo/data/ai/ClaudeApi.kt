package com.vic.recompo.data.ai

import com.vic.recompo.data.ai.dto.ClaudeRequest
import com.vic.recompo.data.ai.dto.ClaudeResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ClaudeApi {
    @POST("v1/messages")
    suspend fun messages(@Body request: ClaudeRequest): ClaudeResponse
}
