package com.vic.recompo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room.Room
import com.vic.recompo.data.db.RecompoDatabase
import com.vic.recompo.data.db.dao.EntradaComidaDao
import com.vic.recompo.data.db.entity.EntradaComida
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
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class EntradaComidaDaoTest {

    private lateinit var db: RecompoDatabase
    private lateinit var dao: EntradaComidaDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RecompoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.entradaComidaDao()
    }

    @After
    fun teardown() = db.close()

    private fun entrada(
        fecha: LocalDate = LocalDate.of(2025, 5, 1),
        slot: SlotComida = SlotComida.COMIDA,
        kcal: Int = 500,
        proteinaG: Double = 40.0,
        grasaG: Double = 15.0,
        carboG: Double = 50.0
    ) = EntradaComida(
        fecha = fecha,
        slot = slot,
        textoLibre = "Pollo con arroz",
        kcal = kcal,
        proteinaG = proteinaG,
        grasaG = grasaG,
        carboG = carboG,
        comidaBaseId = null,
        parseadaPorIA = false,
        timestamp = Instant.now()
    )

    @Test
    fun insert_y_getByFecha_devuelve_entradas_del_dia() = runTest {
        val fecha = LocalDate.of(2025, 5, 1)
        dao.insert(entrada(fecha = fecha))
        dao.insert(entrada(fecha = fecha, slot = SlotComida.CENA))
        dao.insert(entrada(fecha = LocalDate.of(2025, 5, 2)))

        val entradas = dao.getByFecha(fecha).first()
        assertEquals(2, entradas.size)
    }

    @Test
    fun getByFechaYSlot_filtra_por_slot() = runTest {
        val fecha = LocalDate.of(2025, 5, 1)
        dao.insert(entrada(fecha = fecha, slot = SlotComida.DESAYUNO, kcal = 300))
        dao.insert(entrada(fecha = fecha, slot = SlotComida.COMIDA, kcal = 600))
        dao.insert(entrada(fecha = fecha, slot = SlotComida.CENA, kcal = 500))

        val comidas = dao.getByFechaYSlot(fecha, SlotComida.COMIDA).first()
        assertEquals(1, comidas.size)
        assertEquals(600, comidas[0].kcal)
    }

    @Test
    fun getTotalesDelDia_suma_macros_correctamente() = runTest {
        val fecha = LocalDate.of(2025, 5, 1)
        dao.insert(entrada(fecha = fecha, kcal = 500, proteinaG = 40.0, grasaG = 15.0, carboG = 50.0))
        dao.insert(entrada(fecha = fecha, slot = SlotComida.CENA, kcal = 600, proteinaG = 50.0, grasaG = 20.0, carboG = 60.0))

        val totales = dao.getTotalesDelDia(fecha)
        assertNotNull(totales)
        assertEquals(1100, totales!!.kcal)
        assertEquals(90.0, totales.proteinaG, 0.001)
        assertEquals(35.0, totales.grasaG, 0.001)
        assertEquals(110.0, totales.carboG, 0.001)
    }

    @Test
    fun deleteById_elimina_entrada() = runTest {
        val fecha = LocalDate.of(2025, 5, 1)
        val id = dao.insert(entrada(fecha = fecha))
        dao.deleteById(id)
        assertEquals(0, dao.getByFecha(fecha).first().size)
    }

    @Test
    fun getTotalesDelDia_retorna_null_si_no_hay_entradas() = runTest {
        val totales = dao.getTotalesDelDia(LocalDate.of(2025, 1, 1))
        assertNull(totales?.kcal?.let { if (it == 0) null else it })
    }
}
