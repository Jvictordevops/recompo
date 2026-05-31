package com.vic.recompo.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vic.recompo.domain.model.GrupoMuscular
import com.vic.recompo.domain.model.PatronMovimiento

@Entity
data class Ejercicio(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val grupoMuscularPrincipal: GrupoMuscular,
    val gruposSecundarios: String,
    val patron: PatronMovimiento,
    val equipamientoCasa: Boolean,
    val equipamientoGym: Boolean,
    val notasTecnica: String?,
    val activo: Boolean,
    val seedId: String? = null
)
