package com.vic.recompo.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vic.recompo.data.db.entity.TipoSesion
import kotlinx.coroutines.flow.Flow

@Dao
interface TipoSesionDao {
    @Insert suspend fun insert(tipo: TipoSesion): Long
    @Update suspend fun update(tipo: TipoSesion)
    @Delete suspend fun delete(tipo: TipoSesion)

    @Query("SELECT * FROM tipo_sesion ORDER BY id")
    fun getAll(): Flow<List<TipoSesion>>

    @Query("SELECT * FROM tipo_sesion WHERE activo = 1 ORDER BY id")
    fun getActivos(): Flow<List<TipoSesion>>

    @Query("SELECT * FROM tipo_sesion WHERE id = :id")
    suspend fun getById(id: Long): TipoSesion?

    @Query("SELECT COUNT(*) FROM tipo_sesion")
    suspend fun count(): Int
}
