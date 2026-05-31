package com.vic.recompo.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vic.recompo.domain.model.EstadoSesion
import com.vic.recompo.domain.model.OrigenSesion
import java.time.Instant

@Entity
data class Sesion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipoSesionId: Long,
    val fechaEjecutada: Instant?,
    val estado: EstadoSesion,
    val generadaPor: OrigenSesion,
    val notasIA: String?,
    val notasGlobales: String?,
    val rirGlobal: Int?,
    val seedId: String? = null,
    val activo: Boolean = true
)
