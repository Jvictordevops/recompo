package com.vic.recompo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vic.recompo.data.db.entity.Medicion
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface MedicionDao {
    @Insert suspend fun insert(medicion: Medicion): Long
    @Update suspend fun update(medicion: Medicion)

    @Query("DELETE FROM Medicion WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM Medicion ORDER BY fecha DESC")
    fun getAll(): Flow<List<Medicion>>

    @Query("SELECT * FROM Medicion WHERE id = :id")
    suspend fun getById(id: Long): Medicion?

    @Query("SELECT * FROM Medicion ORDER BY fecha DESC LIMIT 1")
    suspend fun getLatest(): Medicion?

    @Query("SELECT * FROM Medicion WHERE fecha = :fecha LIMIT 1")
    suspend fun getByFecha(fecha: LocalDate): Medicion?
}
