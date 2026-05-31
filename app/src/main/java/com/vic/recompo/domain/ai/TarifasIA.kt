package com.vic.recompo.domain.ai

object TarifasIA {

    private data class Tarifa(val inputPorMillon: Double, val outputPorMillon: Double)

    // Precios en USD por millón de tokens. Modelos gratuitos: 0.0.
    private val tarifas = mapOf(
        "claude-sonnet-4-6" to Tarifa(inputPorMillon = 3.0, outputPorMillon = 15.0),
        "claude-haiku-4-5-20251001" to Tarifa(inputPorMillon = 0.8, outputPorMillon = 4.0)
    )

    fun calcularCosteUsd(modelo: String, tokensIn: Int, tokensOut: Int): Double {
        val t = tarifas[modelo] ?: return 0.0
        return tokensIn * t.inputPorMillon / 1_000_000.0 + tokensOut * t.outputPorMillon / 1_000_000.0
    }
}
