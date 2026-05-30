package com.vic.recompo

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room.Room
import com.vic.recompo.data.db.RecompoDatabase
import com.vic.recompo.data.db.dao.TipoSesionDao
import com.vic.recompo.data.db.entity.TipoSesion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class TipoSesionDaoTest {

    private lateinit var db: RecompoDatabase
    private lateinit var dao: TipoSesionDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RecompoDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.tipoSesionDao()
    }

    @After
    fun teardown() = db.close()

    private fun tipo(nombre: String = "A", activo: Boolean = true, esSeed: Boolean = true) =
        TipoSesion(nombre = nombre, descripcion = null, esSeed = esSeed, activo = activo)

    @Test
    fun insert_y_getById_devuelven_mismo_tipo() = runTest {
        val id = dao.insert(tipo("A"))
        val resultado = dao.getById(id)
        assertNotNull(resultado)
        assertEquals("A", resultado!!.nombre)
        assertTrue(resultado.activo)
    }

    @Test
    fun getActivos_excluye_inactivos() = runTest {
        dao.insert(tipo("A", activo = true))
        dao.insert(tipo("B", activo = false))
        dao.insert(tipo("C", activo = true))

        val activos = dao.getActivos().first()
        assertEquals(2, activos.size)
        assert(activos.all { it.activo })
    }

    @Test
    fun getAll_devuelve_todos_incluyendo_inactivos() = runTest {
        dao.insert(tipo("A", activo = true))
        dao.insert(tipo("B", activo = false))

        val todos = dao.getAll().first()
        assertEquals(2, todos.size)
    }

    @Test
    fun update_cambia_nombre() = runTest {
        val id = dao.insert(tipo("A"))
        val original = dao.getById(id)!!
        dao.update(original.copy(nombre = "Empuje"))
        assertEquals("Empuje", dao.getById(id)!!.nombre)
    }

    @Test
    fun toggle_activo_desactiva_tipo() = runTest {
        val id = dao.insert(tipo("A", activo = true))
        val original = dao.getById(id)!!
        dao.update(original.copy(activo = false))
        assertEquals(false, dao.getById(id)!!.activo)
    }

    @Test
    fun count_refleja_total_de_tipos() = runTest {
        assertEquals(0, dao.count())
        dao.insert(tipo("A"))
        dao.insert(tipo("B"))
        assertEquals(2, dao.count())
    }

    @Test
    fun delete_elimina_el_tipo() = runTest {
        val id = dao.insert(tipo("A"))
        val t = dao.getById(id)!!
        dao.delete(t)
        val todos = dao.getAll().first()
        assertEquals(0, todos.size)
    }

    @Test
    fun tipos_no_seed_se_crean_correctamente() = runTest {
        val id = dao.insert(tipo("Cardio", esSeed = false))
        val resultado = dao.getById(id)!!
        assertEquals(false, resultado.esSeed)
        assertEquals("Cardio", resultado.nombre)
    }
}
