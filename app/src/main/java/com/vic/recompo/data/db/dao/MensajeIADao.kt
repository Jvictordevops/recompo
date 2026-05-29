package com.vic.recompo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vic.recompo.data.db.entity.MensajeIA
import kotlinx.coroutines.flow.Flow

@Dao
interface MensajeIADao {
    @Insert suspend fun insert(mensaje: MensajeIA): Long
    @Insert suspend fun insertAll(list: List<MensajeIA>)

    @Query("SELECT * FROM MensajeIA ORDER BY conversacionId, timestamp")
    suspend fun getAll(): List<MensajeIA>

    @Query("SELECT * FROM MensajeIA WHERE conversacionId = :conversacionId ORDER BY timestamp")
    fun getByConversacion(conversacionId: Long): Flow<List<MensajeIA>>

    @Query("DELETE FROM MensajeIA WHERE conversacionId = :conversacionId")
    suspend fun deleteByConversacion(conversacionId: Long)
}
