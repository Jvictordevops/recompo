package com.vic.recompo.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vic.recompo.domain.model.EstadoSerie
import com.vic.recompo.domain.model.MotivoOmision

@Entity(indices = [Index("ejercicioEnSesionId")])
data class Serie(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ejercicioEnSesionId: Long,
    val numero: Int,
    val repsReales: Int?,
    val cargaKg: Double?,
    val rir: Int?,
    val estado: EstadoSerie = EstadoSerie.COMPLETADA,
    val motivoOmision: MotivoOmision? = null
)
