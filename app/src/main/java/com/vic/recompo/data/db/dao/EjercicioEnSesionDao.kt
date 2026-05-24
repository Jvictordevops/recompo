package com.vic.recompo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vic.recompo.data.db.entity.EjercicioEnSesion
import kotlinx.coroutines.flow.Flow

@Dao
interface EjercicioEnSesionDao {
    @Insert suspend fun insert(ejercicioEnSesion: EjercicioEnSesion): Long
    @Insert suspend fun insertAll(list: List<EjercicioEnSesion>)
    @Update suspend fun update(ejercicioEnSesion: EjercicioEnSesion)

    @Query("SELECT * FROM EjercicioEnSesion WHERE sesionId = :sesionId ORDER BY orden")
    fun getBySesion(sesionId: Long): Flow<List<EjercicioEnSesion>>

    @Query("DELETE FROM EjercicioEnSesion WHERE id = :id")
    suspend fun deleteById(id: Long)
}
