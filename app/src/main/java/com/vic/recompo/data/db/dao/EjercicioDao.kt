package com.vic.recompo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vic.recompo.data.db.entity.Ejercicio
import kotlinx.coroutines.flow.Flow

@Dao
interface EjercicioDao {
    @Insert suspend fun insert(ejercicio: Ejercicio): Long
    @Update suspend fun update(ejercicio: Ejercicio)

    @Query("SELECT * FROM Ejercicio ORDER BY nombre")
    fun getAll(): Flow<List<Ejercicio>>

    @Query("SELECT * FROM Ejercicio WHERE activo = 1 ORDER BY nombre")
    fun getAllActivos(): Flow<List<Ejercicio>>

    @Query("SELECT * FROM Ejercicio WHERE id = :id")
    suspend fun getById(id: Long): Ejercicio?

    @Query("SELECT COUNT(*) FROM Ejercicio")
    suspend fun count(): Int

    @Insert
    suspend fun insertAll(list: List<Ejercicio>)

    @Query("SELECT * FROM Ejercicio WHERE nombre = :nombre AND activo = 1 LIMIT 1")
    suspend fun getByNombre(nombre: String): Ejercicio?
}
