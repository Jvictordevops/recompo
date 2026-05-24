package com.vic.recompo.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vic.recompo.domain.model.SlotComida
import java.time.Instant
import java.time.LocalDate

@Entity(indices = [Index("fecha")])
data class EntradaComida(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: LocalDate,
    val slot: SlotComida,
    val textoLibre: String,
    val kcal: Int,
    val proteinaG: Double,
    val grasaG: Double,
    val carboG: Double,
    val comidaBaseId: Long?,
    val parseadaPorIA: Boolean,
    val timestamp: Instant
)
