package com.vic.recompo.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vic.recompo.data.db.entity.Sesion
import com.vic.recompo.domain.model.EstadoSesion
import kotlinx.coroutines.flow.Flow

@Dao
interface SesionDao {
    @Insert suspend fun insert(sesion: Sesion): Long
    @Update suspend fun update(sesion: Sesion)
    @Delete suspend fun delete(sesion: Sesion)

    @Query("SELECT * FROM Sesion WHERE id = :id")
    suspend fun getById(id: Long): Sesion?

    @Query("SELECT * FROM Sesion WHERE seedId = :seedId AND activo = 1 LIMIT 1")
    suspend fun getBySeedId(seedId: String): Sesion?

    @Query("SELECT * FROM Sesion WHERE seedId = :seedId LIMIT 1")
    suspend fun getBySeedIdAny(seedId: String): Sesion?

    // EN_CURSO primero, luego PREPARADA, luego COMPLETADA/OMITIDA por fechaEjecutada DESC
    @Query("""
        SELECT * FROM Sesion WHERE activo = 1 ORDER BY
        CASE estado
            WHEN 'EN_CURSO' THEN 0
            WHEN 'PREPARADA' THEN 1
            WHEN 'COMPLETADA' THEN 2
            ELSE 3
        END,
        fechaEjecutada DESC
    """)
    fun getAll(): Flow<List<Sesion>>

    @Query("SELECT * FROM Sesion WHERE estado IN ('PREPARADA', 'EN_CURSO') AND activo = 1 ORDER BY CASE estado WHEN 'EN_CURSO' THEN 0 ELSE 1 END")
    fun getActivas(): Flow<List<Sesion>>

    @Query("SELECT * FROM Sesion WHERE tipoSesionId = :tipoSesionId AND estado = 'PREPARADA' AND activo = 1 LIMIT 1")
    suspend fun getPreparadaByTipo(tipoSesionId: Long): Sesion?

    @Query("SELECT * FROM Sesion WHERE tipoSesionId = :tipoSesionId AND estado = 'COMPLETADA' AND activo = 1 ORDER BY fechaEjecutada DESC LIMIT :limit")
    suspend fun getCompletadasByTipo(tipoSesionId: Long, limit: Int): List<Sesion>

    @Query("SELECT * FROM Sesion WHERE estado IN ('PREPARADA', 'EN_CURSO') AND activo = 1 ORDER BY CASE estado WHEN 'EN_CURSO' THEN 0 ELSE 1 END")
    fun getPendientes(): Flow<List<Sesion>>
}
