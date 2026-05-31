package com.vic.recompo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vic.recompo.data.db.entity.UsoIA
import kotlinx.coroutines.flow.Flow

@Dao
interface UsoIADao {
    @Insert suspend fun insert(uso: UsoIA): Long

    @Query("SELECT * FROM UsoIA WHERE timestamp >= :desdeEpochMilli ORDER BY timestamp DESC")
    fun getDesde(desdeEpochMilli: Long): Flow<List<UsoIA>>
}
