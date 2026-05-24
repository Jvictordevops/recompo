package com.vic.recompo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room.Room
import com.vic.recompo.data.db.RecompoDatabase
import com.vic.recompo.data.db.dao.MedicionDao
import com.vic.recompo.data.db.entity.Medicion
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
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class MedicionDaoTest {

    private lateinit var db: RecompoDatabase
    private lateinit var dao: MedicionDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RecompoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.medicionDao()
    }

    @After
    fun teardown() = db.close()

    private fun medicion(fecha: LocalDate = LocalDate.of(2025, 1, 15), pesoKg: Double = 91.0) =
        Medicion(
            fecha = fecha,
            pesoKg = pesoKg,
            alturaCmEnLaMedicion = 176,
            grasaPctOverride = false,
            cinturaCm = null, caderaCm = null, cuelloCm = null,
            pechoCm = null, bicepsCm = null, musloCm = null,
            grasaPct = null, masaGrasaKg = null, masaMagraKg = null,
            imc = null, whr = null, faseTexto = null, hito = null, notas = null
        )

    @Test
    fun insert_y_getById_devuelven_misma_medicion() = runTest {
        val id = dao.insert(medicion())
        val resultado = dao.getById(id)
        assertNotNull(resultado)
        assertEquals(91.0, resultado!!.pesoKg, 0.001)
    }

    @Test
    fun getLatest_devuelve_la_mas_reciente() = runTest {
        dao.insert(medicion(LocalDate.of(2025, 1, 10), pesoKg = 92.0))
        dao.insert(medicion(LocalDate.of(2025, 1, 20), pesoKg = 90.5))
        dao.insert(medicion(LocalDate.of(2025, 1, 15), pesoKg = 91.0))

        val latest = dao.getLatest()
        assertNotNull(latest)
        assertEquals(LocalDate.of(2025, 1, 20), latest!!.fecha)
    }

    @Test
    fun getByFecha_devuelve_medicion_correcta() = runTest {
        val fecha = LocalDate.of(2025, 3, 5)
        dao.insert(medicion(fecha, pesoKg = 89.5))
        val resultado = dao.getByFecha(fecha)
        assertNotNull(resultado)
        assertEquals(89.5, resultado!!.pesoKg, 0.001)
    }

    @Test
    fun deleteById_elimina_la_medicion() = runTest {
        val id = dao.insert(medicion())
        dao.deleteById(id)
        assertNull(dao.getById(id))
    }

    @Test
    fun getAll_devuelve_mediciones_ordenadas_por_fecha_desc() = runTest {
        dao.insert(medicion(LocalDate.of(2025, 1, 1), 93.0))
        dao.insert(medicion(LocalDate.of(2025, 2, 1), 91.0))
        dao.insert(medicion(LocalDate.of(2025, 3, 1), 89.0))

        val todas = dao.getAll().first()
        assertEquals(3, todas.size)
        assertEquals(LocalDate.of(2025, 3, 1), todas[0].fecha)
    }
}
