package com.vic.recompo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vic.recompo.data.db.entity.ComidaBase
import com.vic.recompo.domain.model.SlotComida
import kotlinx.coroutines.flow.Flow

@Dao
interface ComidaBaseDao {
    @Insert suspend fun insert(comidaBase: ComidaBase): Long
    @Update suspend fun update(comidaBase: ComidaBase)

    @Query("SELECT * FROM ComidaBase ORDER BY slot, variante")
    fun getAll(): Flow<List<ComidaBase>>

    @Query("SELECT * FROM ComidaBase WHERE activo = 1 ORDER BY slot, variante")
    fun getAllActivos(): Flow<List<ComidaBase>>

    @Query("SELECT * FROM ComidaBase WHERE slot = :slot AND activo = 1 ORDER BY variante")
    fun getBySlot(slot: SlotComida): Flow<List<ComidaBase>>

    @Query("SELECT * FROM ComidaBase WHERE id = :id")
    suspend fun getById(id: Long): ComidaBase?

    @Query("DELETE FROM ComidaBase WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM ComidaBase")
    suspend fun count(): Int

    @Query("SELECT * FROM ComidaBase WHERE variante = :variante LIMIT 1")
    suspend fun getByVariante(variante: String): ComidaBase?

    @Query("SELECT * FROM ComidaBase WHERE variante LIKE '%' || :query || '%' AND activo = 1 ORDER BY variante LIMIT 5")
    suspend fun searchByVariante(query: String): List<ComidaBase>

    @Query("UPDATE ComidaBase SET activo = :activo WHERE id = :id")
    suspend fun setActivo(id: Long, activo: Boolean)
}
