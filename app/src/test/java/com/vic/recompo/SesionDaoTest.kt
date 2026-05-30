package com.vic.recompo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room.Room
import com.vic.recompo.data.db.RecompoDatabase
import com.vic.recompo.data.db.dao.SesionDao
import com.vic.recompo.data.db.dao.TipoSesionDao
import com.vic.recompo.data.db.entity.Sesion
import com.vic.recompo.data.db.entity.TipoSesion
import com.vic.recompo.domain.model.EstadoSesion
import com.vic.recompo.domain.model.OrigenSesion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
class SesionDaoTest {

    private lateinit var db: RecompoDatabase
    private lateinit var dao: SesionDao
    private lateinit var tipoSesionDao: TipoSesionDao

    private var tipoIdA: Long = 0
    private var tipoIdB: Long = 0

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RecompoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.sesionDao()
        tipoSesionDao = db.tipoSesionDao()
        runBlocking {
            tipoIdA = tipoSesionDao.insert(TipoSesion(nombre = "A", descripcion = null, esSeed = true))
            tipoIdB = tipoSesionDao.insert(TipoSesion(nombre = "B", descripcion = null, esSeed = true))
        }
    }

    @After
    fun teardown() = db.close()

    private fun sesion(
        tipoId: Long = tipoIdA,
        estado: EstadoSesion = EstadoSesion.PREPARADA
    ) = Sesion(
        tipoSesionId = tipoId,
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
        assertEquals(tipoIdA, resultado!!.tipoSesionId)
    }

    @Test
    fun getActivas_solo_devuelve_preparada_y_en_curso() = runTest {
        dao.insert(sesion(estado = EstadoSesion.PREPARADA))
        dao.insert(sesion(tipoId = tipoIdB, estado = EstadoSesion.EN_CURSO))
        dao.insert(sesion(estado = EstadoSesion.COMPLETADA))
        dao.insert(sesion(tipoId = tipoIdB, estado = EstadoSesion.OMITIDA))

        val activas = dao.getActivas().first()
        assertEquals(2, activas.size)
        assert(activas.all { it.estado == EstadoSesion.PREPARADA || it.estado == EstadoSesion.EN_CURSO })
    }

    @Test
    fun getActivas_ordena_en_curso_primero() = runTest {
        dao.insert(sesion(estado = EstadoSesion.PREPARADA))
        dao.insert(sesion(tipoId = tipoIdB, estado = EstadoSesion.EN_CURSO))

        val activas = dao.getActivas().first()
        assertEquals(EstadoSesion.EN_CURSO, activas[0].estado)
        assertEquals(EstadoSesion.PREPARADA, activas[1].estado)
    }

    @Test
    fun getPendientes_devuelve_preparada_y_en_curso() = runTest {
        dao.insert(sesion(estado = EstadoSesion.PREPARADA))
        dao.insert(sesion(tipoId = tipoIdB, estado = EstadoSesion.EN_CURSO))
        dao.insert(sesion(estado = EstadoSesion.COMPLETADA))

        val pendientes = dao.getPendientes().first()
        assertEquals(2, pendientes.size)
        assert(pendientes.all { it.estado == EstadoSesion.PREPARADA || it.estado == EstadoSesion.EN_CURSO })
    }

    @Test
    fun getPreparadaByTipo_devuelve_solo_la_de_ese_tipo() = runTest {
        dao.insert(sesion(tipoId = tipoIdA, estado = EstadoSesion.PREPARADA))
        dao.insert(sesion(tipoId = tipoIdB, estado = EstadoSesion.PREPARADA))

        val preparadaA = dao.getPreparadaByTipo(tipoIdA)
        assertNotNull(preparadaA)
        assertEquals(tipoIdA, preparadaA!!.tipoSesionId)
    }

    @Test
    fun getPreparadaByTipo_devuelve_null_si_no_existe() = runTest {
        dao.insert(sesion(tipoId = tipoIdA, estado = EstadoSesion.COMPLETADA))

        val preparada = dao.getPreparadaByTipo(tipoIdA)
        assertNull(preparada)
    }

    @Test
    fun getCompletadasByTipo_respeta_limit() = runTest {
        repeat(5) { dao.insert(sesion(tipoId = tipoIdA, estado = EstadoSesion.COMPLETADA)) }

        val resultados = dao.getCompletadasByTipo(tipoIdA, limit = 3)
        assertEquals(3, resultados.size)
        assert(resultados.all { it.estado == EstadoSesion.COMPLETADA && it.tipoSesionId == tipoIdA })
    }

    @Test
    fun getCompletadasByTipo_no_mezcla_tipos() = runTest {
        dao.insert(sesion(tipoId = tipoIdA, estado = EstadoSesion.COMPLETADA))
        dao.insert(sesion(tipoId = tipoIdB, estado = EstadoSesion.COMPLETADA))

        val resultados = dao.getCompletadasByTipo(tipoIdA, limit = 10)
        assertEquals(1, resultados.size)
        assertEquals(tipoIdA, resultados[0].tipoSesionId)
    }

    @Test
    fun update_modifica_estado_correctamente() = runTest {
        val id = dao.insert(sesion())
        val original = dao.getById(id)!!
        dao.update(original.copy(estado = EstadoSesion.COMPLETADA))
        assertEquals(EstadoSesion.COMPLETADA, dao.getById(id)!!.estado)
    }

    @Test
    fun getAll_ordena_en_curso_preparada_completada() = runTest {
        dao.insert(sesion(tipoId = tipoIdA, estado = EstadoSesion.COMPLETADA))
        dao.insert(sesion(tipoId = tipoIdB, estado = EstadoSesion.EN_CURSO))
        dao.insert(sesion(tipoId = tipoIdA, estado = EstadoSesion.PREPARADA))

        val todas = dao.getAll().first()
        assertEquals(3, todas.size)
        assertEquals(EstadoSesion.EN_CURSO, todas[0].estado)
        assertEquals(EstadoSesion.PREPARADA, todas[1].estado)
        assertEquals(EstadoSesion.COMPLETADA, todas[2].estado)
    }
}
