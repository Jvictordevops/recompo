package com.vic.recompo.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.vic.recompo.data.db.entity.Sesion
import com.vic.recompo.domain.model.EstadoSesion
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SesionDao {
    @Insert suspend fun insert(sesion: Sesion): Long
    @Update suspend fun update(sesion: Sesion)

    @Query("SELECT * FROM Sesion WHERE id = :id")
    suspend fun getById(id: Long): Sesion?

    @Query("SELECT * FROM Sesion ORDER BY fechaPrevista DESC")
    fun getAll(): Flow<List<Sesion>>

    @Query("SELECT * FROM Sesion WHERE fechaPrevista = :fecha")
    fun getByFecha(fecha: LocalDate): Flow<List<Sesion>>

    @Query("SELECT * FROM Sesion WHERE estado = :estado ORDER BY fechaPrevista")
    fun getByEstado(estado: EstadoSesion): Flow<List<Sesion>>

    @Query("SELECT * FROM Sesion WHERE estado IN ('PLANIFICADA', 'EN_CURSO') ORDER BY fechaPrevista")
    fun getPendientes(): Flow<List<Sesion>>
}
