package com.vic.recompo.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(entity = ComidaBase::class, parentColumns = ["id"], childColumns = ["comidaBaseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Ingrediente::class, parentColumns = ["id"], childColumns = ["ingredienteId"])
    ],
    indices = [Index("comidaBaseId"), Index("ingredienteId")]
)
data class PlantillaIngrediente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val comidaBaseId: Long,
    val ingredienteId: Long,
    val cantidadG: Double
)
