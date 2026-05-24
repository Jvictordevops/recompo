package com.vic.recompo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vic.recompo.data.db.entity.Conversacion
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversacionDao {
    @Insert suspend fun insert(conversacion: Conversacion): Long
    @Update suspend fun update(conversacion: Conversacion)

    @Query("SELECT * FROM Conversacion ORDER BY fechaCreacion DESC")
    fun getAll(): Flow<List<Conversacion>>

    @Query("SELECT * FROM Conversacion WHERE id = :id")
    suspend fun getById(id: Long): Conversacion?
}
