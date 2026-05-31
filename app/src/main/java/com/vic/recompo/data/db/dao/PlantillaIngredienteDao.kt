package com.vic.recompo.data.db.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.vic.recompo.data.db.entity.Ingrediente
import com.vic.recompo.data.db.entity.PlantillaIngrediente

data class PlantillaConIngrediente(
    @Embedded val plantilla: PlantillaIngrediente,
    @Relation(parentColumn = "ingredienteId", entityColumn = "id")
    val ingrediente: Ingrediente
)

@Dao
interface PlantillaIngredienteDao {
    @Insert suspend fun insert(pi: PlantillaIngrediente): Long
    @Insert suspend fun insertAll(items: List<PlantillaIngrediente>)
    @Update suspend fun update(pi: PlantillaIngrediente)

    @Query("DELETE FROM PlantillaIngrediente WHERE comidaBaseId = :comidaBaseId")
    suspend fun deleteByComidaBase(comidaBaseId: Long)

    @Query("SELECT count(*) FROM PlantillaIngrediente")
    suspend fun count(): Int

    @Transaction
    @Query("SELECT * FROM PlantillaIngrediente WHERE comidaBaseId = :comidaBaseId ORDER BY id")
    suspend fun getByComidaBase(comidaBaseId: Long): List<PlantillaConIngrediente>

    @Query("SELECT DISTINCT comidaBaseId FROM PlantillaIngrediente")
    suspend fun getAllComidaBaseIdsConIngredientes(): List<Long>
}
