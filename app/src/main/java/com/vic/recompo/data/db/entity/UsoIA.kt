package com.vic.recompo.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.vic.recompo.domain.model.FuncionIA
import com.vic.recompo.domain.model.ProveedorIA
import java.time.Instant

@Entity(indices = [Index("timestamp"), Index("funcion")])
data class UsoIA(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Instant,
    val funcion: FuncionIA,
    val proveedor: ProveedorIA,
    val modelo: String,
    val tokensIn: Int,
    val tokensOut: Int,
    val costeUsd: Double
)
