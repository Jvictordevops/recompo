package com.vic.recompo.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vic.recompo.domain.model.RolMensaje
import java.time.Instant

@Entity(indices = [Index("conversacionId")])
data class MensajeIA(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversacionId: Long,
    val rol: RolMensaje,
    val contenido: String,
    val tokensIn: Int?,
    val tokensOut: Int?,
    val timestamp: Instant
)
