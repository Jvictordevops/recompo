package com.vic.recompo.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index("ejercicioEnSesionId")])
data class Serie(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ejercicioEnSesionId: Long,
    val numero: Int,
    val repsReales: Int,
    val cargaKg: Double,
    val rir: Int,
    val completada: Boolean
)
