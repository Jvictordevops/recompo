package com.vic.recompo.domain.ai

sealed class ParseComidaResult {
    data class NecesitaAclaracion(val preguntas: List<String>) : ParseComidaResult()
    data class Calculado(
        val kcal: Int,
        val proteinaG: Double,
        val grasaG: Double,
        val carboG: Double,
        val confianza: String,
        val supuestos: String?
    ) : ParseComidaResult()
}
