package com.vic.recompo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room.Room
import com.vic.recompo.data.db.RecompoDatabase
import com.vic.recompo.data.db.dao.SesionDao
import com.vic.recompo.data.db.entity.Sesion
import com.vic.recompo.domain.model.EstadoSesion
import com.vic.recompo.domain.model.OrigenSesion
import com.vic.recompo.domain.model.TipoSesion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class SesionDaoTest {

    private lateinit var db: RecompoDatabase
    private lateinit var dao: SesionDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RecompoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.sesionDao()
    }

    @After
    fun teardown() = db.close()

    private fun sesion(
        tipo: TipoSesion = TipoSesion.A,
        fecha: LocalDate = LocalDate.of(2025, 6, 2),
        estado: EstadoSesion = EstadoSesion.PLANIFICADA
    ) = Sesion(
        tipo = tipo,
        fechaPrevista = fecha,
        fechaEjecutada = null,
        estado = estado,
        generadaPor = OrigenSesion.MANUAL,
        notasIA = null,
        notasGlobales = null,
        rirGlobal = null
    )

    @Test
    fun insert_y_getById_devuelven_misma_sesion() = runTest {
        val id = dao.insert(sesion())
        val resultado = dao.getById(id)
        assertNotNull(resultado)
        assertEquals(TipoSesion.A, resultado!!.tipo)
    }

    @Test
    fun getByFecha_devuelve_sesiones_del_dia() = runTest {
        val fecha = LocalDate.of(2025, 6, 2)
        dao.insert(sesion(fecha = fecha))
        dao.insert(sesion(tipo = TipoSesion.B, fecha = LocalDate.of(2025, 6, 3)))

        val sesiones = dao.getByFecha(fecha).first()
        assertEquals(1, sesiones.size)
        assertEquals(TipoSesion.A, sesiones[0].tipo)
    }

    @Test
    fun getPendientes_excluye_completadas_y_omitidas() = runTest {
        dao.insert(sesion(estado = EstadoSesion.PLANIFICADA))
        dao.insert(sesion(tipo = TipoSesion.B, estado = EstadoSesion.EN_CURSO))
        dao.insert(sesion(tipo = TipoSesion.C, estado = EstadoSesion.COMPLETADA))
        dao.insert(sesion(estado = EstadoSesion.OMITIDA, fecha = LocalDate.of(2025, 6, 5)))

        val pendientes = dao.getPendientes().first()
        assertEquals(2, pendientes.size)
        assert(pendientes.all { it.estado in listOf(EstadoSesion.PLANIFICADA, EstadoSesion.EN_CURSO) })
    }

    @Test
    fun update_modifica_estado_correctamente() = runTest {
        val id = dao.insert(sesion())
        val original = dao.getById(id)!!
        dao.update(original.copy(estado = EstadoSesion.COMPLETADA))
        assertEquals(EstadoSesion.COMPLETADA, dao.getById(id)!!.estado)
    }

    @Test
    fun getAll_devuelve_sesiones_ordenadas_por_fecha_desc() = runTest {
        dao.insert(sesion(fecha = LocalDate.of(2025, 6, 1)))
        dao.insert(sesion(fecha = LocalDate.of(2025, 6, 3)))
        dao.insert(sesion(fecha = LocalDate.of(2025, 6, 2)))

        val todas = dao.getAll().first()
        assertEquals(3, todas.size)
        assertEquals(LocalDate.of(2025, 6, 3), todas[0].fechaPrevista)
    }
}
