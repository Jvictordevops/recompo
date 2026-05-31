package com.vic.recompo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vic.recompo.data.db.RecompoDatabase
import com.vic.recompo.data.db.dao.IngredienteDao
import com.vic.recompo.data.db.dao.PlantillaIngredienteDao
import com.vic.recompo.data.db.entity.ComidaBase
import com.vic.recompo.data.db.entity.Ingrediente
import com.vic.recompo.data.db.entity.PlantillaIngrediente
import com.vic.recompo.domain.model.Fiabilidad
import com.vic.recompo.domain.model.SlotComida
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class IngredienteDaoTest {

    private lateinit var db: RecompoDatabase
    private lateinit var ingredienteDao: IngredienteDao
    private lateinit var plantillaIngredienteDao: PlantillaIngredienteDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RecompoDatabase::class.java
        ).allowMainThreadQueries().build()
        ingredienteDao = db.ingredienteDao()
        plantillaIngredienteDao = db.plantillaIngredienteDao()
    }

    @After
    fun teardown() = db.close()

    private fun ingrediente(
        nombre: String = "Avena",
        kcal100g: Double = 370.0,
        prot100g: Double = 13.0,
        grasa100g: Double = 7.0,
        carbo100g: Double = 60.0,
        fiabilidad: Fiabilidad = Fiabilidad.ESTIMADO
    ) = Ingrediente(
        nombre = nombre,
        kcal100g = kcal100g,
        prot100g = prot100g,
        grasa100g = grasa100g,
        carbo100g = carbo100g,
        fiabilidad = fiabilidad
    )

    private fun comidaBase(variante: String = "Leche+avena") = ComidaBase(
        slot = SlotComida.DESAYUNO,
        variante = variante,
        kcal = 400,
        proteinaG = 30.0,
        grasaG = 8.0,
        carboG = 50.0,
        ingredientesTexto = variante,
        activo = true
    )

    @Test
    fun insert_y_count_devuelve_total_correcto() = runTest {
        assertEquals(0, ingredienteDao.count())
        ingredienteDao.insert(ingrediente("Avena"))
        ingredienteDao.insert(ingrediente("Leche"))
        assertEquals(2, ingredienteDao.count())
    }

    @Test
    fun getAll_devuelve_activos_ordenados_por_nombre() = runTest {
        ingredienteDao.insert(ingrediente("Yogur"))
        ingredienteDao.insert(ingrediente("Avena"))
        ingredienteDao.insert(ingrediente("Leche").copy(activo = false))

        val activos = ingredienteDao.getAll().first()
        assertEquals(2, activos.size)
        assertEquals("Avena", activos[0].nombre)
        assertEquals("Yogur", activos[1].nombre)
    }

    @Test
    fun getByNombre_encuentra_ingrediente_exacto() = runTest {
        ingredienteDao.insert(ingrediente("Proteína whey"))
        val found = ingredienteDao.getByNombre("Proteína whey")
        assertNotNull(found)
        assertEquals("Proteína whey", found!!.nombre)
    }

    @Test
    fun getByNombre_devuelve_null_si_no_existe() = runTest {
        assertNull(ingredienteDao.getByNombre("Ingrediente inexistente"))
    }

    @Test
    fun search_devuelve_resultados_por_substring() = runTest {
        ingredienteDao.insertAll(listOf(
            ingrediente("Pechuga de pollo"),
            ingrediente("Pechuga de pavo"),
            ingrediente("Lomo embuchado")
        ))
        val results = ingredienteDao.search("Pechuga")
        assertEquals(2, results.size)
    }

    @Test
    fun search_no_devuelve_inactivos() = runTest {
        ingredienteDao.insert(ingrediente("Atún en aceite").copy(activo = false))
        val results = ingredienteDao.search("Atún")
        assertEquals(0, results.size)
    }

    @Test
    fun getByComidaBase_devuelve_ingredientes_con_relacion() = runTest {
        val avenaId = ingredienteDao.insert(ingrediente("Avena", kcal100g = 370.0, prot100g = 13.0, grasa100g = 7.0, carbo100g = 60.0))
        val lecheId = ingredienteDao.insert(ingrediente("Leche", kcal100g = 47.0, prot100g = 3.2, grasa100g = 1.5, carbo100g = 4.8))

        val cbId = db.comidaBaseDao().insert(comidaBase("Leche+avena"))
        plantillaIngredienteDao.insertAll(listOf(
            PlantillaIngrediente(comidaBaseId = cbId, ingredienteId = avenaId, cantidadG = 40.0),
            PlantillaIngrediente(comidaBaseId = cbId, ingredienteId = lecheId, cantidadG = 250.0)
        ))

        val items = plantillaIngredienteDao.getByComidaBase(cbId)
        assertEquals(2, items.size)
        val nombres = items.map { it.ingrediente.nombre }.toSet()
        assertEquals(setOf("Avena", "Leche"), nombres)
    }

    @Test
    fun calcularTotales_con_cantidades_reales() = runTest {
        // Avena 40g: kcal = 370*40/100 = 148, prot = 13*40/100 = 5.2
        // Leche 250g: kcal = 47*250/100 = 117.5, prot = 3.2*250/100 = 8.0
        val avenaId = ingredienteDao.insert(ingrediente("Avena", kcal100g = 370.0, prot100g = 13.0, grasa100g = 7.0, carbo100g = 60.0))
        val lecheId = ingredienteDao.insert(ingrediente("Leche", kcal100g = 47.0, prot100g = 3.2, grasa100g = 1.5, carbo100g = 4.8))

        val cbId = db.comidaBaseDao().insert(comidaBase())
        plantillaIngredienteDao.insertAll(listOf(
            PlantillaIngrediente(comidaBaseId = cbId, ingredienteId = avenaId, cantidadG = 40.0),
            PlantillaIngrediente(comidaBaseId = cbId, ingredienteId = lecheId, cantidadG = 250.0)
        ))

        val items = plantillaIngredienteDao.getByComidaBase(cbId)
        val totalKcal = items.sumOf { (it.ingrediente.kcal100g * it.plantilla.cantidadG / 100).toInt() }
        val totalProt = items.sumOf { it.ingrediente.prot100g * it.plantilla.cantidadG / 100 }

        assertEquals(265, totalKcal)  // 148 + 117
        assertEquals(13.2, totalProt, 0.01)  // 5.2 + 8.0
    }

    @Test
    fun deleteByComidaBase_elimina_plantilla_ingredientes() = runTest {
        val ingId = ingredienteDao.insert(ingrediente())
        val cbId = db.comidaBaseDao().insert(comidaBase())
        plantillaIngredienteDao.insert(PlantillaIngrediente(comidaBaseId = cbId, ingredienteId = ingId, cantidadG = 100.0))

        assertEquals(1, plantillaIngredienteDao.count())
        plantillaIngredienteDao.deleteByComidaBase(cbId)
        assertEquals(0, plantillaIngredienteDao.count())
    }

    @Test
    fun cascade_delete_comidaBase_elimina_plantilla_ingredientes() = runTest {
        val ingId = ingredienteDao.insert(ingrediente())
        val cbId = db.comidaBaseDao().insert(comidaBase())
        plantillaIngredienteDao.insert(PlantillaIngrediente(comidaBaseId = cbId, ingredienteId = ingId, cantidadG = 100.0))

        assertEquals(1, plantillaIngredienteDao.count())
        db.comidaBaseDao().deleteById(cbId)
        assertEquals(0, plantillaIngredienteDao.count())
    }
}
