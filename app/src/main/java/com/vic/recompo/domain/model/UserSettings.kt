package com.vic.recompo.domain.model

import java.time.Instant
import java.time.LocalDate

data class UserSettings(
    val nombre: String,
    val fechaNacimiento: LocalDate,
    val sexo: Sexo,
    val alturaCm: Int,
    val fechaInicioPlan: LocalDate,
    val pesoInicialKg: Double,
    val pesoObjetivoKg: Double,
    val faseActual: String,
    val kcalBaseDia: Int,
    val proteinaObjetivoG: Int,
    val carpetaBackupUri: String?,
    val ultimoBackupOk: Instant?,
    val ultimoBackupError: String?,
    val ultimoBackupBytes: Long?
)
