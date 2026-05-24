package com.vic.recompo.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vic.recompo.domain.model.SlotComida

@Entity
data class ComidaBase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slot: SlotComida,
    val variante: String,
    val kcal: Int,
    val proteinaG: Double,
    val grasaG: Double,
    val carboG: Double,
    val ingredientesTexto: String,
    val activo: Boolean
)
