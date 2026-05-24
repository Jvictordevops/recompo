package com.vic.recompo.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.vic.recompo.domain.model.TipoConversacion
import java.time.Instant

@Entity
data class Conversacion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tipo: TipoConversacion,
    val fechaCreacion: Instant,
    val titulo: String?
)
