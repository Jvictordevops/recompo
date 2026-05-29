package com.vic.recompo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vic.recompo.data.db.entity.Serie
import kotlinx.coroutines.flow.Flow

@Dao
interface SerieDao {
    @Insert suspend fun insert(serie: Serie): Long
    @Insert suspend fun insertAll(list: List<Serie>)
    @Update suspend fun update(serie: Serie)

    @Query("SELECT * FROM Serie ORDER BY ejercicioEnSesionId, numero")
    suspend fun getAll(): List<Serie>

    @Query("SELECT * FROM Serie WHERE ejercicioEnSesionId = :ejercicioEnSesionId ORDER BY numero")
    fun getByEjercicioEnSesion(ejercicioEnSesionId: Long): Flow<List<Serie>>

    @Query("DELETE FROM Serie WHERE ejercicioEnSesionId = :ejercicioEnSesionId")
    suspend fun deleteByEjercicioEnSesion(ejercicioEnSesionId: Long)
}
