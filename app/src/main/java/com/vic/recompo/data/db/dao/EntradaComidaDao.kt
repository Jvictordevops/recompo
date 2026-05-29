package com.vic.recompo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vic.recompo.data.db.entity.EntradaComida
import com.vic.recompo.domain.model.SlotComida
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

data class TotalesComida(
    val kcal: Int,
    val proteinaG: Double,
    val grasaG: Double,
    val carboG: Double
)

@Dao
interface EntradaComidaDao {
    @Insert suspend fun insert(entrada: EntradaComida): Long
    @Update suspend fun update(entrada: EntradaComida)

    @Query("DELETE FROM EntradaComida WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM EntradaComida ORDER BY fecha, timestamp")
    suspend fun getAll(): List<EntradaComida>

    @Query("SELECT * FROM EntradaComida WHERE fecha = :fecha ORDER BY timestamp")
    fun getByFecha(fecha: LocalDate): Flow<List<EntradaComida>>

    @Query("SELECT * FROM EntradaComida WHERE fecha = :fecha AND slot = :slot ORDER BY timestamp")
    fun getByFechaYSlot(fecha: LocalDate, slot: SlotComida): Flow<List<EntradaComida>>

    @Query("""
        SELECT SUM(kcal) as kcal, SUM(proteinaG) as proteinaG,
               SUM(grasaG) as grasaG, SUM(carboG) as carboG
        FROM EntradaComida WHERE fecha = :fecha
    """)
    suspend fun getTotalesDelDia(fecha: LocalDate): TotalesComida?
}
