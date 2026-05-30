package com.vic.recompo.domain.usecase

import com.vic.recompo.data.db.entity.Sesion
import com.vic.recompo.ui.entreno.EjercicioConSeries

data class SesionConDetalle(
    val sesion: Sesion,
    val tipoNombre: String,
    val ejerciciosConSeries: List<EjercicioConSeries>
)

data class ProximaSesionPropuesta(
    val razonamiento: String,
    val ejercicios: List<EjercicioPropuesto>
)

data class EjercicioPropuesto(
    val nombreEjercicio: String,
    val seriesObjetivo: Int,
    val repsMin: Int,
    val repsMax: Int,
    val cargaObjetivoKg: Double?,
    val notas: String?
)
