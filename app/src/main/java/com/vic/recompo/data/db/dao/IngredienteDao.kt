package com.vic.recompo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vic.recompo.data.db.entity.Ingrediente
import kotlinx.coroutines.flow.Flow

@Dao
interface IngredienteDao {
    @Insert suspend fun insert(ingrediente: Ingrediente): Long
    @Insert suspend fun insertAll(ingredientes: List<Ingrediente>)
    @Update suspend fun update(ingrediente: Ingrediente)

    @Query("SELECT count(*) FROM Ingrediente")
    suspend fun count(): Int

    @Query("SELECT * FROM Ingrediente WHERE activo = 1 ORDER BY nombre")
    fun getAll(): Flow<List<Ingrediente>>

    @Query("SELECT * FROM Ingrediente WHERE id = :id")
    suspend fun getById(id: Long): Ingrediente?

    @Query("SELECT * FROM Ingrediente WHERE nombre = :nombre AND activo = 1 LIMIT 1")
    suspend fun getByNombre(nombre: String): Ingrediente?

    @Query("SELECT * FROM Ingrediente WHERE nombre LIKE '%' || :query || '%' AND activo = 1 ORDER BY nombre LIMIT 10")
    suspend fun search(query: String): List<Ingrediente>
}
