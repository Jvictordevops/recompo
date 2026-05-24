package com.vic.recompo.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(indices = [Index("fecha")])
data class Medicion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: LocalDate,
    val pesoKg: Double,
    val cinturaCm: Double?,
    val caderaCm: Double?,
    val cuelloCm: Double?,
    val pechoCm: Double?,
    val bicepsCm: Double?,
    val musloCm: Double?,
    val alturaCmEnLaMedicion: Int,
    val grasaPct: Double?,
    val grasaPctOverride: Boolean,
    val masaGrasaKg: Double?,
    val masaMagraKg: Double?,
    val imc: Double?,
    val whr: Double?,
    val faseTexto: String?,
    val hito: String?,
    val notas: String?
)
