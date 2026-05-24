package com.vic.recompo.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(indices = [Index("fecha")])
data class Actividad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: LocalDate,
    val tipo: String,
    val descripcion: String?,
    val duracionMin: Int?,
    val kcalQuemadas: Int
)
