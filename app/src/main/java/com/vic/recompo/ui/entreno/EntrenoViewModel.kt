package com.vic.recompo.ui.entreno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vic.recompo.data.db.dao.EjercicioDao
import com.vic.recompo.data.db.dao.EjercicioEnSesionDao
import com.vic.recompo.data.db.dao.SerieDao
import com.vic.recompo.data.db.dao.SesionDao
import com.vic.recompo.data.db.entity.Ejercicio
import com.vic.recompo.data.db.entity.EjercicioEnSesion
import com.vic.recompo.data.db.entity.Serie
import com.vic.recompo.data.db.entity.Sesion
import com.vic.recompo.domain.model.EstadoSesion
import com.vic.recompo.domain.model.OrigenSesion
import android.content.Context
import com.vic.recompo.domain.model.TipoSesion
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

enum class EntrenoFase { LISTA, PRE_SESION, EN_CURSO, POST_SESION }

data class EjercicioConSeries(
    val ejercicioEnSesion: EjercicioEnSesion,
    val ejercicio: Ejercicio,
    val series: List<Serie>
)

data class FormAgregarEjercicio(
    val ejercicioId: Long? = null,
    val seriesObjetivo: String = "3",
    val repsMin: String = "8",
    val repsMax: String = "12",
    val cargaKg: String = "",
    val notas: String = ""
)

data class FormSerie(
    val ejercicioEnSesionId: Long,
    val numero: Int,
    val reps: String = "",
    val cargaKg: String = "",
    val rir: Int = 2,
    val esUltimaSerie: Boolean = false
)

data class EntrenoUiState(
    val sesiones: List<Sesion> = emptyList(),
    val fase: EntrenoFase = EntrenoFase.LISTA,
    val sesionActual: Sesion? = null,
    val ejerciciosConSeries: List<EjercicioConSeries> = emptyList(),
    val ejerciciosDisponibles: List<Ejercicio> = emptyList(),
    val dialogCrearSesion: Boolean = false,
    val tipoNuevaSesion: TipoSesion = TipoSesion.A,
    val fechaNuevaSesion: LocalDate = LocalDate.now(),
    val showDatePicker: Boolean = false,
    val errorCrearSesion: String? = null,
    val dialogAgregarEjercicio: FormAgregarEjercicio? = null,
    val dialogSerie: FormSerie? = null,
    val dialogConfirmarBorrado: Sesion? = null,
    val notasGlobales: String = "",
    val rirGlobal: String = ""
)

class EntrenoViewModel(
    private val context: Context,
    private val sesionDao: SesionDao,
    private val ejercicioEnSesionDao: EjercicioEnSesionDao,
    private val serieDao: SerieDao,
    private val ejercicioDao: EjercicioDao
) : ViewModel() {

    private val _state = MutableStateFlow(EntrenoUiState())
    val uiState: StateFlow<EntrenoUiState> = _state

    init {
        viewModelScope.launch {
            sesionDao.getAll().collect { sesiones ->
                _state.update { it.copy(sesiones = sesiones) }
            }
        }
    }

    // --- LISTA ---

    fun abrirCrearSesion() = _state.update {
        it.copy(
            dialogCrearSesion = true,
            tipoNuevaSesion = TipoSesion.A,
            fechaNuevaSesion = LocalDate.now(),
            errorCrearSesion = null
        )
    }

    fun cerrarCrearSesion() = _state.update {
        it.copy(dialogCrearSesion = false, errorCrearSesion = null)
    }

    fun onTipoNuevaSesionChanged(tipo: TipoSesion) = _state.update {
        it.copy(tipoNuevaSesion = tipo, errorCrearSesion = null)
    }

    fun onFechaNuevaSesionChanged(fecha: LocalDate) = _state.update {
        it.copy(fechaNuevaSesion = fecha, errorCrearSesion = null)
    }

    fun mostrarDatePicker(show: Boolean) = _state.update { it.copy(showDatePicker = show) }

    fun crearSesion() {
        val st = _state.value
        viewModelScope.launch {
            val existente = sesionDao.getByFechaYTipo(st.fechaNuevaSesion, st.tipoNuevaSesion)
            if (existente != null) {
                _state.update {
                    it.copy(errorCrearSesion = "Ya existe una sesión ${st.tipoNuevaSesion.name} para esa fecha")
                }
                return@launch
            }
            val id = sesionDao.insert(
                Sesion(
                    tipo = st.tipoNuevaSesion,
                    fechaPrevista = st.fechaNuevaSesion,
                    fechaEjecutada = null,
                    estado = EstadoSesion.PLANIFICADA,
                    generadaPor = OrigenSesion.MANUAL,
                    notasIA = null,
                    notasGlobales = null,
                    rirGlobal = null
                )
            )
            val sesion = sesionDao.getById(id) ?: return@launch
            poblarDesdeTemplate(sesion)
            navegarAPreSesion(sesion)
        }
    }

    private suspend fun poblarDesdeTemplate(sesion: Sesion) {
        val json = context.assets.open("seed/sesiones_template.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            if (obj.getString("tipo") != sesion.tipo.name) continue
            val ejerciciosJson = obj.getJSONArray("ejercicios")
            for (j in 0 until ejerciciosJson.length()) {
                val ej = ejerciciosJson.getJSONObject(j)
                val ejercicio = ejercicioDao.getByNombre(ej.getString("nombreEjercicio")) ?: continue
                ejercicioEnSesionDao.insert(
                    EjercicioEnSesion(
                        sesionId = sesion.id,
                        ejercicioId = ejercicio.id,
                        orden = j + 1,
                        seriesObjetivo = ej.getInt("seriesObjetivo"),
                        repsObjetivoMin = ej.getInt("repsObjetivoMin"),
                        repsObjetivoMax = ej.getInt("repsObjetivoMax"),
                        cargaObjetivoKg = ej.getDouble("cargaObjetivoKg").takeIf { it > 0.0 },
                        notas = ej.optString("notas").takeIf { it.isNotEmpty() },
                        rir = null
                    )
                )
            }
            break
        }
    }

    fun abrirPreSesion(sesion: Sesion) {
        viewModelScope.launch {
            when (sesion.estado) {
                EstadoSesion.COMPLETADA -> navegarAPostSesion(sesion)
                EstadoSesion.EN_CURSO -> navegarAEnCurso(sesion)
                else -> navegarAPreSesion(sesion)
            }
        }
    }

    fun volverALista() = _state.update {
        it.copy(fase = EntrenoFase.LISTA, sesionActual = null, ejerciciosConSeries = emptyList())
    }

    fun pedirConfirmarBorrado(sesion: Sesion) = _state.update {
        it.copy(dialogConfirmarBorrado = sesion)
    }

    fun cerrarConfirmarBorrado() = _state.update { it.copy(dialogConfirmarBorrado = null) }

    fun confirmarEliminarSesion() {
        val sesion = _state.value.dialogConfirmarBorrado ?: return
        viewModelScope.launch {
            val ejerciciosIds = ejercicioEnSesionDao.getIdsBySesion(sesion.id)
            ejerciciosIds.forEach { serieDao.deleteByEjercicioEnSesion(it) }
            ejercicioEnSesionDao.deleteBySesion(sesion.id)
            sesionDao.delete(sesion)
            _state.update { it.copy(dialogConfirmarBorrado = null) }
        }
    }

    // --- PRE-SESIÓN ---

    fun abrirAgregarEjercicio() {
        viewModelScope.launch {
            val ejercicios = ejercicioDao.getAllActivos().first()
            _state.update {
                it.copy(ejerciciosDisponibles = ejercicios, dialogAgregarEjercicio = FormAgregarEjercicio())
            }
        }
    }

    fun cerrarAgregarEjercicio() = _state.update { it.copy(dialogAgregarEjercicio = null) }

    fun onFormAgregarEjercicioChanged(form: FormAgregarEjercicio) =
        _state.update { it.copy(dialogAgregarEjercicio = form) }

    fun confirmarAgregarEjercicio() {
        val sesionId = _state.value.sesionActual?.id ?: return
        val form = _state.value.dialogAgregarEjercicio ?: return
        val ejercicioId = form.ejercicioId ?: return
        val series = form.seriesObjetivo.toIntOrNull() ?: return
        val repsMin = form.repsMin.toIntOrNull() ?: return
        val repsMax = form.repsMax.toIntOrNull() ?: return
        viewModelScope.launch {
            val orden = _state.value.ejerciciosConSeries.size + 1
            ejercicioEnSesionDao.insert(
                EjercicioEnSesion(
                    sesionId = sesionId,
                    ejercicioId = ejercicioId,
                    orden = orden,
                    seriesObjetivo = series,
                    repsObjetivoMin = repsMin,
                    repsObjetivoMax = repsMax,
                    cargaObjetivoKg = form.cargaKg.toDoubleOrNull(),
                    notas = form.notas.trim().takeIf { it.isNotEmpty() },
                    rir = null
                )
            )
            recargarEjercicios()
            _state.update { it.copy(dialogAgregarEjercicio = null) }
        }
    }

    fun eliminarEjercicio(ejercicioEnSesionId: Long) {
        viewModelScope.launch {
            serieDao.deleteByEjercicioEnSesion(ejercicioEnSesionId)
            ejercicioEnSesionDao.deleteById(ejercicioEnSesionId)
            recargarEjercicios()
        }
    }

    fun iniciarSesion() {
        val sesion = _state.value.sesionActual ?: return
        viewModelScope.launch {
            val updated = sesion.copy(estado = EstadoSesion.EN_CURSO, fechaEjecutada = Instant.now())
            sesionDao.update(updated)
            _state.update { it.copy(fase = EntrenoFase.EN_CURSO, sesionActual = updated) }
        }
    }

    // --- EN CURSO ---

    fun abrirRegistrarSerie(ejercicioEnSesionId: Long) {
        val ec = _state.value.ejerciciosConSeries.find { it.ejercicioEnSesion.id == ejercicioEnSesionId }
            ?: return
        val numero = ec.series.size + 1
        val esUltima = numero >= ec.ejercicioEnSesion.seriesObjetivo
        _state.update {
            it.copy(
                dialogSerie = FormSerie(
                    ejercicioEnSesionId = ejercicioEnSesionId,
                    numero = numero,
                    esUltimaSerie = esUltima
                )
            )
        }
    }

    fun cerrarDialogSerie() = _state.update { it.copy(dialogSerie = null) }

    fun onFormSerieChanged(form: FormSerie) = _state.update { it.copy(dialogSerie = form) }

    fun guardarSerie() {
        val form = _state.value.dialogSerie ?: return
        val reps = form.reps.toIntOrNull() ?: return
        val carga = form.cargaKg.toDoubleOrNull() ?: return
        viewModelScope.launch {
            serieDao.insert(
                Serie(
                    ejercicioEnSesionId = form.ejercicioEnSesionId,
                    numero = form.numero,
                    repsReales = reps,
                    cargaKg = carga,
                    rir = form.rir,
                    completada = true
                )
            )
            if (form.esUltimaSerie) {
                val ec = _state.value.ejerciciosConSeries
                    .find { it.ejercicioEnSesion.id == form.ejercicioEnSesionId }
                if (ec != null) {
                    ejercicioEnSesionDao.update(ec.ejercicioEnSesion.copy(rir = form.rir))
                }
            }
            recargarEjercicios()
            _state.update { it.copy(dialogSerie = null) }
        }
    }

    fun irAPostSesion() = _state.update {
        it.copy(fase = EntrenoFase.POST_SESION, notasGlobales = "", rirGlobal = "")
    }

    fun volverAEnCurso() = _state.update { it.copy(fase = EntrenoFase.EN_CURSO) }

    // --- POST-SESIÓN ---

    fun onNotasGlobalesChanged(v: String) = _state.update { it.copy(notasGlobales = v) }
    fun onRirGlobalChanged(v: String) = _state.update { it.copy(rirGlobal = v) }

    fun cerrarSesion() {
        val sesion = _state.value.sesionActual ?: return
        viewModelScope.launch {
            sesionDao.update(
                sesion.copy(
                    estado = EstadoSesion.COMPLETADA,
                    notasGlobales = _state.value.notasGlobales.trim().takeIf { it.isNotEmpty() },
                    rirGlobal = _state.value.rirGlobal.toIntOrNull()
                )
            )
            _state.update {
                it.copy(fase = EntrenoFase.LISTA, sesionActual = null, ejerciciosConSeries = emptyList())
            }
        }
    }

    // --- helpers ---

    private suspend fun navegarAPreSesion(sesion: Sesion) {
        val ejerciciosConSeries = cargarEjerciciosConSeries(sesion.id)
        _state.update {
            it.copy(
                fase = EntrenoFase.PRE_SESION,
                sesionActual = sesion,
                ejerciciosConSeries = ejerciciosConSeries,
                dialogCrearSesion = false,
                errorCrearSesion = null
            )
        }
    }

    private suspend fun navegarAEnCurso(sesion: Sesion) {
        val ejerciciosConSeries = cargarEjerciciosConSeries(sesion.id)
        _state.update {
            it.copy(
                fase = EntrenoFase.EN_CURSO,
                sesionActual = sesion,
                ejerciciosConSeries = ejerciciosConSeries
            )
        }
    }

    private suspend fun navegarAPostSesion(sesion: Sesion) {
        val ejerciciosConSeries = cargarEjerciciosConSeries(sesion.id)
        _state.update {
            it.copy(
                fase = EntrenoFase.POST_SESION,
                sesionActual = sesion,
                ejerciciosConSeries = ejerciciosConSeries,
                notasGlobales = sesion.notasGlobales.orEmpty(),
                rirGlobal = sesion.rirGlobal?.toString().orEmpty()
            )
        }
    }

    private suspend fun cargarEjerciciosConSeries(sesionId: Long): List<EjercicioConSeries> {
        val ejerciciosEnSesion = ejercicioEnSesionDao.getBySesion(sesionId).first()
        return ejerciciosEnSesion.mapNotNull { ees ->
            val ejercicio = ejercicioDao.getById(ees.ejercicioId) ?: return@mapNotNull null
            val series = serieDao.getByEjercicioEnSesion(ees.id).first()
            EjercicioConSeries(ees, ejercicio, series)
        }
    }

    private suspend fun recargarEjercicios() {
        val sesionId = _state.value.sesionActual?.id ?: return
        _state.update { it.copy(ejerciciosConSeries = cargarEjerciciosConSeries(sesionId)) }
    }
}

class EntrenoViewModelFactory(
    private val context: Context,
    private val sesionDao: SesionDao,
    private val ejercicioEnSesionDao: EjercicioEnSesionDao,
    private val serieDao: SerieDao,
    private val ejercicioDao: EjercicioDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        EntrenoViewModel(context, sesionDao, ejercicioEnSesionDao, serieDao, ejercicioDao) as T
}
