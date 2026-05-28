package com.vic.recompo.domain.validation

import java.time.LocalDate

object UserSettingsValidation {

    const val ERROR_NOMBRE_OBLIGATORIO = "El nombre es obligatorio"
    const val ERROR_FECHA_NACIMIENTO = "Fecha de nacimiento inválida (formato AAAA-MM-DD)"
    const val ERROR_FECHA_INICIO_PLAN = "Fecha de inicio inválida (formato AAAA-MM-DD)"
    const val ERROR_SEXO_OBLIGATORIO = "Selecciona sexo biológico"
    const val ERROR_ALTURA = "Altura inválida (100–250 cm)"
    const val ERROR_PESO_INICIAL = "Peso inicial inválido"
    const val ERROR_PESO_OBJETIVO = "Peso objetivo inválido"
    const val ERROR_FASE_OBLIGATORIA = "La fase actual es obligatoria"
    const val ERROR_KCAL = "Kcal base día inválido"
    const val ERROR_PROTEINA = "Proteína objetivo inválida"

    fun parseAltura(input: String): Result<Int> {
        val v = input.toIntOrNull()
        return if (v != null && v in 100..250) Result.success(v)
        else Result.failure(IllegalArgumentException(ERROR_ALTURA))
    }

    fun parsePesoKg(input: String, error: String = ERROR_PESO_INICIAL): Result<Double> {
        val v = input.replace(',', '.').toDoubleOrNull()
        return if (v != null && v > 0.0) Result.success(v)
        else Result.failure(IllegalArgumentException(error))
    }

    fun parseKcal(input: String): Result<Int> {
        val v = input.toIntOrNull()
        return if (v != null && v > 0) Result.success(v)
        else Result.failure(IllegalArgumentException(ERROR_KCAL))
    }

    fun parseProteinaG(input: String): Result<Int> {
        val v = input.toIntOrNull()
        return if (v != null && v > 0) Result.success(v)
        else Result.failure(IllegalArgumentException(ERROR_PROTEINA))
    }

    fun parseLocalDate(input: String, error: String): Result<LocalDate> = runCatching {
        LocalDate.parse(input)
    }.recoverCatching { throw IllegalArgumentException(error) }

    fun parseNombre(input: String): Result<String> {
        val trimmed = input.trim()
        return if (trimmed.isNotEmpty()) Result.success(trimmed)
        else Result.failure(IllegalArgumentException(ERROR_NOMBRE_OBLIGATORIO))
    }

    fun parseFase(input: String): Result<String> {
        val trimmed = input.trim()
        return if (trimmed.isNotEmpty()) Result.success(trimmed)
        else Result.failure(IllegalArgumentException(ERROR_FASE_OBLIGATORIA))
    }
}
