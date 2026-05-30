package com.vic.recompo.domain.ai

data class ParseComidaResult(
    val kcal: Int,
    val proteinaG: Double,
    val grasaG: Double,
    val carboG: Double,
    val confianza: String,
    val notas: String?
)
