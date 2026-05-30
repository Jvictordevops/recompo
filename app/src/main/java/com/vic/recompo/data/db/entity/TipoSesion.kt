package com.vic.recompo.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tipo_sesion")
data class TipoSesion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val descripcion: String?,
    val esSeed: Boolean,
    val activo: Boolean = true
)
