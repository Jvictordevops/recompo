package com.vic.recompo.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("sesionId")])
data class EjercicioEnSesion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sesionId: Long,
    val ejercicioId: Long,
    val orden: Int,
    val seriesObjetivo: Int,
    val repsObjetivoMin: Int,
    val repsObjetivoMax: Int,
    val cargaObjetivoKg: Double?,
    val notas: String?,
    val rir: Int? = null
)
