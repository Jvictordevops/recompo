package com.vic.recompo.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vic.recompo.domain.model.Fiabilidad

@Entity
data class Ingrediente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val kcal100g: Double,
    val prot100g: Double,
    val grasa100g: Double,
    val carbo100g: Double,
    val gramosPorUnidad: Int? = null,
    val nombreUnidad: String? = null,
    val fiabilidad: Fiabilidad = Fiabilidad.ESTIMADO,
    val activo: Boolean = true
)
