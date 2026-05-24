package com.vic.recompo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vic.recompo.data.db.entity.Actividad
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ActividadDao {
    @Insert suspend fun insert(actividad: Actividad): Long
    @Update suspend fun update(actividad: Actividad)

    @Query("DELETE FROM Actividad WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM Actividad WHERE fecha = :fecha ORDER BY id")
    fun getByFecha(fecha: LocalDate): Flow<List<Actividad>>
}
