package com.vic.recompo.ui.entreno

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vic.recompo.data.ai.ClaudeApi
import com.vic.recompo.data.db.dao.EjercicioDao
import com.vic.recompo.data.db.dao.EjercicioEnSesionDao
import com.vic.recompo.data.db.dao.SerieDao
import com.vic.recompo.data.db.dao.SesionDao
import com.vic.recompo.data.db.dao.TipoSesionDao
import com.vic.recompo.data.db.entity.Ejercicio
import com.vic.recompo.data.db.entity.EjercicioEnSesion
import com.vic.recompo.data.db.entity.Serie
import com.vic.recompo.data.db.entity.Sesion
import com.vic.recompo.data.db.entity.TipoSesion
import com.vic.recompo.domain.model.EstadoSerie
import com.vic.recompo.domain.model.EstadoSesion
import com.vic.recompo.domain.model.MotivoOmision
import com.vic.recompo.domain.model.OrigenSesion
import com.vic.recompo.domain.usecase.EjercicioPropuesto
import com.vic.recompo.domain.usecase.GenerarSesionUseCase
import com.vic.recompo.domain.usecase.ProximaSesionPropuesta
import com.vic.recompo.domain.usecase.SesionConDetalle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

enum class EntrenoFase { LISTA, PRE_SESION, EN_CURSO, POST_SESION }

data class EjercicioConSeries(
    val ejercicioEnSesion: EjercicioEnSesion,
    val ejercicio: Ejercicio,
    val series: List<Serie>
)

data class SesionConTipo(
    val sesion: Sesion,
    val tipoNombre: String
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
    val sesiones: List<SesionConTipo> = emptyList(),
    val tiposSesion: List<TipoSesion> = emptyList(),
    val fase: EntrenoFase = EntrenoFase.LISTA,
    val sesionActual: SesionConTipo? = null,
    val ejerciciosConSeries: List<EjercicioConSeries> = emptyList(),
    val ejerciciosDisponibles: List<Ejercicio> = emptyList(),
    // dialog nueva sesión (manual o con IA)
    val dialogNuevaSesion: Boolean = false,
    val tipoNuevaSesionId: Long? = null,
    val errorNuevaSesion: String? = null,
    // dialogs confirmación
    val dialogConfirmarBorrado: SesionConTipo? = null,
    val dialogConfirmarReemplazarPreparada: Long? = null, // tipoSesionId
    // generación IA
    val generandoSesion: Boolean = false,
    val propuestaSesion: ProximaSesionPropuesta? = null,
    val propuestaTipoId: Long? = null,
    val errorGeneracion: String? = null,
    // dialog contexto sin histórico
    val dialogContextoSinHistorico: Long? = null, // tipoSesionId
    // sesión en curso
    val dialogAgregarEjercicio: FormAgregarEjercicio? = null,
    val dialogSerie: FormSerie? = null,
    val dialogSaltarSerie: Long? = null, // ejercicioEnSesionId
    val motivoOmisionSeleccionado: MotivoOmision? = null,
    // post-sesión
    val notasGlobales: String = "",
    val rirGlobal: String = "",
    // gestión tipos
    val dialogGestionTipos: Boolean = false,
    val dialogCrearTipo: Boolean = false,
    val nombreNuevoTipo: String = "",
    val dialogRenombrarTipo: TipoSesion? = null,
    val nombreRenombrar: String = "",
    val dialogCancelarSesion: Boolean = false,
    val dialogSesionVacia: Boolean = false,
    val dialogEnCursoConflicto: SesionConTipo? = null
)

class EntrenoViewModel(
    private val context: Context,
    private val sesionDao: SesionDao,
    private val ejercicioEnSesionDao: EjercicioEnSesionDao,
    private val serieDao: SerieDao,
    private val ejercicioDao: EjercicioDao,
    private val tipoSesionDao: TipoSesionDao,
    private val generarSesionUseCase: GenerarSesionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(EntrenoUiState())
    val uiState: StateFlow<EntrenoUiState> = _state

    init {
        viewModelScope.launch {
            combine(
                sesionDao.getAll(),
                tipoSesionDao.getActivos()
            ) { sesiones, tipos ->
                val tipoMap = tipos.associateBy { it.id }
                val sesionesConTipo = sesiones.map { sesion ->
                    SesionConTipo(sesion, tipoMap[sesion.tipoSesionId]?.nombre ?: "?")
                }
                sesionesConTipo to tipos
            }.collect { (sesiones, tipos) ->
                _state.update { it.copy(sesiones = sesiones, tiposSesion = tipos) }
            }
        }
    }

    // ── LISTA ──────────────────────────────────────────────────────────────────

    fun abrirNuevaSesion() = _state.update {
        val defaultTipoId = it.tiposSesion.firstOrNull()?.id
        it.copy(dialogNuevaSesion = true, tipoNuevaSesionId = defaultTipoId, errorNuevaSesion = null)
    }

    fun cerrarNuevaSesion() = _state.update {
        it.copy(dialogNuevaSesion = false, errorNuevaSesion = null)
    }

    fun onTipoNuevaSesionChanged(tipoId: Long) = _state.update {
        it.copy(tipoNuevaSesionId = tipoId, errorNuevaSesion = null)
    }

    fun crearSesionManual() {
        val tipoId = _state.value.tipoNuevaSesionId ?: return
        viewModelScope.launch {
            val id = sesionDao.insert(
                Sesion(
                    tipoSesionId = tipoId,
                    fechaEjecutada = null,
                    estado = EstadoSesion.PREPARADA,
                    generadaPor = OrigenSesion.MANUAL,
                    notasIA = null,
                    notasGlobales = null,
                    rirGlobal = null
                )
            )
            copiarEjerciciosDeUltimaSesion(tipoId, id)
            val sesion = sesionDao.getById(id) ?: return@launch
            val tipo = tipoSesionDao.getById(tipoId)
            navegarAPreSesion(SesionConTipo(sesion, tipo?.nombre ?: "?"))
        }
    }

    fun duplicarSesion(sesionConTipo: SesionConTipo) {
        viewModelScope.launch {
            val id = sesionDao.insert(
                sesionConTipo.sesion.copy(
                    id = 0,
                    estado = EstadoSesion.PREPARADA,
                    fechaEjecutada = null,
                    notasGlobales = null,
                    rirGlobal = null,
                    seedId = null
                )
            )
            val ejercicios = ejercicioEnSesionDao.getBySesion(sesionConTipo.sesion.id).first()
            ejercicios.forEachIndexed { idx, ees ->
                ejercicioEnSesionDao.insert(ees.copy(id = 0, sesionId = id, orden = idx + 1, rir = null))
            }
            val sesion = sesionDao.getById(id) ?: return@launch
            val tipo = tipoSesionDao.getById(sesion.tipoSesionId)
            navegarAPreSesion(SesionConTipo(sesion, tipo?.nombre ?: "?"))
        }
    }

    fun iniciarGeneracionIA() {
        val tipoId = _state.value.tipoNuevaSesionId ?: return
        _state.update { it.copy(dialogNuevaSesion = false) }
        viewModelScope.launch {
            val preparadaExistente = sesionDao.getPreparadaByTipo(tipoId)
            if (preparadaExistente != null) {
                _state.update { it.copy(dialogConfirmarReemplazarPreparada = tipoId) }
                return@launch
            }
            val historial = cargarHistorial(tipoId)
            if (historial.isEmpty()) {
                _state.update { it.copy(dialogContextoSinHistorico = tipoId) }
            } else {
                generarConHistorial(tipoId, historial, null)
            }
        }
    }

    fun confirmarReemplazarPreparada() {
        val tipoId = _state.value.dialogConfirmarReemplazarPreparada ?: return
        _state.update { it.copy(dialogConfirmarReemplazarPreparada = null) }
        viewModelScope.launch {
            val preparada = sesionDao.getPreparadaByTipo(tipoId)
            if (preparada != null) {
                sesionDao.update(preparada.copy(activo = false))
            }
            val historial = cargarHistorial(tipoId)
            if (historial.isEmpty()) {
                _state.update { it.copy(dialogContextoSinHistorico = tipoId) }
            } else {
                generarConHistorial(tipoId, historial, null)
            }
        }
    }

    fun cancelarReemplazarPreparada() = _state.update {
        it.copy(dialogConfirmarReemplazarPreparada = null)
    }

    fun confirmarContextoSinHistorico(equipamiento: String, tiempoMinutos: Int?) {
        val tipoId = _state.value.dialogContextoSinHistorico ?: return
        _state.update { it.copy(dialogContextoSinHistorico = null) }
        val contexto = GenerarSesionUseCase.ContextoSinHistorico(equipamiento, tiempoMinutos)
        viewModelScope.launch { generarConHistorial(tipoId, emptyList(), contexto) }
    }

    fun cancelarContextoSinHistorico() = _state.update { it.copy(dialogContextoSinHistorico = null) }

    private suspend fun generarConHistorial(
        tipoId: Long,
        historial: List<SesionConDetalle>,
        contexto: GenerarSesionUseCase.ContextoSinHistorico?
    ) {
        val tipoNombre = tipoSesionDao.getById(tipoId)?.nombre ?: return
        _state.update { it.copy(generandoSesion = true, errorGeneracion = null, propuestaSesion = null, propuestaTipoId = tipoId) }
        val resultado = generarSesionUseCase.generar(tipoNombre, historial, contexto)
        when (resultado) {
            is GenerarSesionUseCase.Resultado.Exito -> _state.update {
                it.copy(generandoSesion = false, propuestaSesion = resultado.propuesta)
            }
            is GenerarSesionUseCase.Resultado.Fallo -> _state.update {
                it.copy(
                    generandoSesion = false,
                    errorGeneracion = resultado.mensaje,
                    propuestaSesion = resultado.fallback
                )
            }
        }
    }

    fun aceptarPropuesta() {
        val propuesta = _state.value.propuestaSesion ?: return
        val tipoId = _state.value.propuestaTipoId ?: return
        viewModelScope.launch {
            val id = sesionDao.insert(
                Sesion(
                    tipoSesionId = tipoId,
                    fechaEjecutada = null,
                    estado = EstadoSesion.PREPARADA,
                    generadaPor = OrigenSesion.IA,
                    notasIA = propuesta.razonamiento,
                    notasGlobales = null,
                    rirGlobal = null
                )
            )
            insertarEjerciciosDePropuesta(id, propuesta.ejercicios)
            _state.update { it.copy(propuestaSesion = null, propuestaTipoId = null, errorGeneracion = null) }
            val sesion = sesionDao.getById(id) ?: return@launch
            val tipo = tipoSesionDao.getById(tipoId)
            navegarAPreSesion(SesionConTipo(sesion, tipo?.nombre ?: "?"))
        }
    }

    fun regenerarPropuesta() {
        val tipoId = _state.value.propuestaTipoId ?: return
        _state.update { it.copy(propuestaSesion = null, errorGeneracion = null) }
        viewModelScope.launch {
            val historial = cargarHistorial(tipoId)
            generarConHistorial(tipoId, historial, null)
        }
    }

    fun descartarPropuesta() = _state.update {
        it.copy(propuestaSesion = null, propuestaTipoId = null, errorGeneracion = null, generandoSesion = false)
    }

    fun usarFallback() {
        val tipoId = _state.value.propuestaTipoId ?: return
        viewModelScope.launch {
            val historial = cargarHistorial(tipoId)
            val fallback = generarSesionUseCase.generarFallback(historial) ?: return@launch
            _state.update { it.copy(propuestaSesion = fallback, errorGeneracion = null) }
        }
    }

    private suspend fun cargarHistorial(tipoId: Long): List<SesionConDetalle> {
        val tipo = tipoSesionDao.getById(tipoId) ?: return emptyList()
        val sesiones = sesionDao.getCompletadasByTipo(tipoId, GenerarSesionUseCase.MAX_HISTORIAL)
        return sesiones.map { sesion ->
            val ejerciciosConSeries = cargarEjerciciosConSeries(sesion.id)
            SesionConDetalle(sesion, tipo.nombre, ejerciciosConSeries)
        }
    }

    private suspend fun insertarEjerciciosDePropuesta(sesionId: Long, ejercicios: List<EjercicioPropuesto>) {
        ejercicios.forEachIndexed { idx, ep ->
            val ejercicio = ejercicioDao.getByNombre(ep.nombreEjercicio) ?: return@forEachIndexed
            ejercicioEnSesionDao.insert(
                EjercicioEnSesion(
                    sesionId = sesionId,
                    ejercicioId = ejercicio.id,
                    orden = idx + 1,
                    seriesObjetivo = ep.seriesObjetivo,
                    repsObjetivoMin = ep.repsMin,
                    repsObjetivoMax = ep.repsMax,
                    cargaObjetivoKg = ep.cargaObjetivoKg,
                    notas = ep.notas,
                    rir = null
                )
            )
        }
    }

    fun abrirPreSesion(sesionConTipo: SesionConTipo) {
        viewModelScope.launch {
            when (sesionConTipo.sesion.estado) {
                EstadoSesion.COMPLETADA -> navegarAPostSesion(sesionConTipo)
                EstadoSesion.EN_CURSO -> navegarAEnCurso(sesionConTipo)
                else -> navegarAPreSesion(sesionConTipo)
            }
        }
    }

    fun volverALista() = _state.update {
        it.copy(fase = EntrenoFase.LISTA, sesionActual = null, ejerciciosConSeries = emptyList())
    }

    fun pedirConfirmarBorrado(sesionConTipo: SesionConTipo) = _state.update {
        it.copy(dialogConfirmarBorrado = sesionConTipo)
    }

    fun cerrarConfirmarBorrado() = _state.update { it.copy(dialogConfirmarBorrado = null) }

    fun confirmarEliminarSesion() {
        val sesionConTipo = _state.value.dialogConfirmarBorrado ?: return
        viewModelScope.launch {
            sesionDao.update(sesionConTipo.sesion.copy(activo = false))
            _state.update { it.copy(dialogConfirmarBorrado = null) }
        }
    }

    // ── PRE-SESIÓN ─────────────────────────────────────────────────────────────

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
        val sesionId = _state.value.sesionActual?.sesion?.id ?: return
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
        val sesionConTipo = _state.value.sesionActual ?: return
        viewModelScope.launch {
            val enCurso = _state.value.sesiones.firstOrNull { it.sesion.estado == EstadoSesion.EN_CURSO }
            if (enCurso != null && enCurso.sesion.id != sesionConTipo.sesion.id) {
                _state.update { it.copy(dialogEnCursoConflicto = enCurso) }
                return@launch
            }
            val updated = sesionConTipo.sesion.copy(
                estado = EstadoSesion.EN_CURSO,
                fechaEjecutada = Instant.now()
            )
            sesionDao.update(updated)
            _state.update {
                it.copy(
                    fase = EntrenoFase.EN_CURSO,
                    sesionActual = sesionConTipo.copy(sesion = updated)
                )
            }
        }
    }

    fun reanudarSesionEnCurso() {
        val enCurso = _state.value.dialogEnCursoConflicto ?: return
        _state.update { it.copy(dialogEnCursoConflicto = null) }
        viewModelScope.launch { navegarAEnCurso(enCurso) }
    }

    fun cancelarEnCursoYIniciar() {
        val enCurso = _state.value.dialogEnCursoConflicto ?: return
        val sesionConTipo = _state.value.sesionActual ?: return
        _state.update { it.copy(dialogEnCursoConflicto = null) }
        viewModelScope.launch {
            sesionDao.update(enCurso.sesion.copy(estado = EstadoSesion.OMITIDA))
            val updated = sesionConTipo.sesion.copy(
                estado = EstadoSesion.EN_CURSO,
                fechaEjecutada = Instant.now()
            )
            sesionDao.update(updated)
            _state.update {
                it.copy(
                    fase = EntrenoFase.EN_CURSO,
                    sesionActual = sesionConTipo.copy(sesion = updated)
                )
            }
        }
    }

    // ── EN CURSO ───────────────────────────────────────────────────────────────

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
        val carga = form.cargaKg.toDoubleOrNull()
        viewModelScope.launch {
            serieDao.insert(
                Serie(
                    ejercicioEnSesionId = form.ejercicioEnSesionId,
                    numero = form.numero,
                    repsReales = reps,
                    cargaKg = carga,
                    rir = form.rir,
                    estado = EstadoSerie.COMPLETADA
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

    fun abrirSaltarSerie(ejercicioEnSesionId: Long) = _state.update {
        it.copy(dialogSaltarSerie = ejercicioEnSesionId, motivoOmisionSeleccionado = null)
    }

    fun cerrarSaltarSerie() = _state.update {
        it.copy(dialogSaltarSerie = null, motivoOmisionSeleccionado = null)
    }

    fun onMotivoOmisionChanged(motivo: MotivoOmision?) = _state.update {
        it.copy(motivoOmisionSeleccionado = motivo)
    }

    fun confirmarSaltarSerie() {
        val ejercicioEnSesionId = _state.value.dialogSaltarSerie ?: return
        val ec = _state.value.ejerciciosConSeries.find { it.ejercicioEnSesion.id == ejercicioEnSesionId }
            ?: return
        val numero = ec.series.size + 1
        viewModelScope.launch {
            serieDao.insert(
                Serie(
                    ejercicioEnSesionId = ejercicioEnSesionId,
                    numero = numero,
                    repsReales = null,
                    cargaKg = null,
                    rir = null,
                    estado = EstadoSerie.OMITIDA,
                    motivoOmision = _state.value.motivoOmisionSeleccionado
                )
            )
            recargarEjercicios()
            _state.update { it.copy(dialogSaltarSerie = null, motivoOmisionSeleccionado = null) }
        }
    }

    fun irAPostSesion() {
        val totalSeries = _state.value.ejerciciosConSeries.sumOf { it.series.size }
        if (totalSeries == 0) {
            _state.update { it.copy(dialogSesionVacia = true) }
            return
        }
        _state.update { it.copy(fase = EntrenoFase.POST_SESION, notasGlobales = "", rirGlobal = "") }
    }

    fun cerrarSesionVaciaySeguir() = _state.update {
        it.copy(dialogSesionVacia = false, fase = EntrenoFase.POST_SESION, notasGlobales = "", rirGlobal = "")
    }

    fun descartarSesionVacia() {
        val sesionConTipo = _state.value.sesionActual ?: return
        viewModelScope.launch {
            sesionDao.update(sesionConTipo.sesion.copy(activo = false))
            _state.update {
                it.copy(
                    dialogSesionVacia = false,
                    fase = EntrenoFase.LISTA,
                    sesionActual = null,
                    ejerciciosConSeries = emptyList()
                )
            }
        }
    }

    fun pedirCancelarSesion() = _state.update { it.copy(dialogCancelarSesion = true) }
    fun cerrarCancelarSesion() = _state.update { it.copy(dialogCancelarSesion = false) }

    fun confirmarCancelarSesion() {
        val sesionConTipo = _state.value.sesionActual ?: return
        viewModelScope.launch {
            // Discard series from this aborted attempt, keep the session plan
            _state.value.ejerciciosConSeries.forEach { ec ->
                serieDao.deleteByEjercicioEnSesion(ec.ejercicioEnSesion.id)
                if (ec.ejercicioEnSesion.rir != null) {
                    ejercicioEnSesionDao.update(ec.ejercicioEnSesion.copy(rir = null))
                }
            }
            // Revert to PREPARADA — the session is not lost, just not started
            sesionDao.update(sesionConTipo.sesion.copy(
                estado = EstadoSesion.PREPARADA,
                fechaEjecutada = null
            ))
            _state.update {
                it.copy(
                    dialogCancelarSesion = false,
                    fase = EntrenoFase.LISTA,
                    sesionActual = null,
                    ejerciciosConSeries = emptyList()
                )
            }
        }
    }

    fun volverAEnCurso() = _state.update { it.copy(fase = EntrenoFase.EN_CURSO) }

    fun volverDesdePostSesion() {
        val estado = _state.value.sesionActual?.sesion?.estado
        if (estado == EstadoSesion.EN_CURSO) {
            _state.update { it.copy(fase = EntrenoFase.EN_CURSO) }
        } else {
            _state.update { it.copy(fase = EntrenoFase.LISTA, sesionActual = null, ejerciciosConSeries = emptyList()) }
        }
    }

    // ── POST-SESIÓN ────────────────────────────────────────────────────────────

    fun onNotasGlobalesChanged(v: String) = _state.update { it.copy(notasGlobales = v) }
    fun onRirGlobalChanged(v: String) = _state.update { it.copy(rirGlobal = v) }

    fun cerrarSesion() {
        val sesionConTipo = _state.value.sesionActual ?: return
        viewModelScope.launch {
            sesionDao.update(
                sesionConTipo.sesion.copy(
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

    // ── GESTIÓN TIPOS ──────────────────────────────────────────────────────────

    fun abrirGestionTipos() = _state.update { it.copy(dialogGestionTipos = true) }
    fun cerrarGestionTipos() = _state.update { it.copy(dialogGestionTipos = false) }

    fun abrirCrearTipo() = _state.update { it.copy(dialogCrearTipo = true, nombreNuevoTipo = "") }
    fun cerrarCrearTipo() = _state.update { it.copy(dialogCrearTipo = false, nombreNuevoTipo = "") }
    fun onNombreNuevoTipoChanged(v: String) = _state.update { it.copy(nombreNuevoTipo = v) }

    fun confirmarCrearTipo() {
        val nombre = _state.value.nombreNuevoTipo.trim().takeIf { it.isNotEmpty() } ?: return
        viewModelScope.launch {
            tipoSesionDao.insert(TipoSesion(nombre = nombre, descripcion = null, esSeed = false))
            _state.update { it.copy(dialogCrearTipo = false, nombreNuevoTipo = "") }
        }
    }

    fun abrirRenombrarTipo(tipo: TipoSesion) = _state.update {
        it.copy(dialogRenombrarTipo = tipo, nombreRenombrar = tipo.nombre)
    }

    fun cerrarRenombrarTipo() = _state.update { it.copy(dialogRenombrarTipo = null, nombreRenombrar = "") }
    fun onNombreRenombrarChanged(v: String) = _state.update { it.copy(nombreRenombrar = v) }

    fun confirmarRenombrarTipo() {
        val tipo = _state.value.dialogRenombrarTipo ?: return
        val nombre = _state.value.nombreRenombrar.trim().takeIf { it.isNotEmpty() } ?: return
        viewModelScope.launch {
            tipoSesionDao.update(tipo.copy(nombre = nombre))
            _state.update { it.copy(dialogRenombrarTipo = null, nombreRenombrar = "") }
        }
    }

    fun toggleActivoTipo(tipo: TipoSesion) {
        viewModelScope.launch { tipoSesionDao.update(tipo.copy(activo = !tipo.activo)) }
    }

    // ── HELPERS ────────────────────────────────────────────────────────────────

    private suspend fun copiarEjerciciosDeUltimaSesion(tipoId: Long, nuevaSesionId: Long) {
        val ultima = sesionDao.getCompletadasByTipo(tipoId, 1).firstOrNull() ?: return
        val ejercicios = ejercicioEnSesionDao.getBySesion(ultima.id).first()
        ejercicios.forEachIndexed { idx, ees ->
            ejercicioEnSesionDao.insert(ees.copy(id = 0, sesionId = nuevaSesionId, orden = idx + 1, rir = null))
        }
    }

    private suspend fun navegarAPreSesion(sesionConTipo: SesionConTipo) {
        val ejerciciosConSeries = cargarEjerciciosConSeries(sesionConTipo.sesion.id)
        _state.update {
            it.copy(
                fase = EntrenoFase.PRE_SESION,
                sesionActual = sesionConTipo,
                ejerciciosConSeries = ejerciciosConSeries,
                dialogNuevaSesion = false,
                errorNuevaSesion = null
            )
        }
    }

    private suspend fun navegarAEnCurso(sesionConTipo: SesionConTipo) {
        val ejerciciosConSeries = cargarEjerciciosConSeries(sesionConTipo.sesion.id)
        _state.update {
            it.copy(
                fase = EntrenoFase.EN_CURSO,
                sesionActual = sesionConTipo,
                ejerciciosConSeries = ejerciciosConSeries
            )
        }
    }

    private suspend fun navegarAPostSesion(sesionConTipo: SesionConTipo) {
        val ejerciciosConSeries = cargarEjerciciosConSeries(sesionConTipo.sesion.id)
        _state.update {
            it.copy(
                fase = EntrenoFase.POST_SESION,
                sesionActual = sesionConTipo,
                ejerciciosConSeries = ejerciciosConSeries,
                notasGlobales = sesionConTipo.sesion.notasGlobales.orEmpty(),
                rirGlobal = sesionConTipo.sesion.rirGlobal?.toString().orEmpty()
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
        val sesionId = _state.value.sesionActual?.sesion?.id ?: return
        _state.update { it.copy(ejerciciosConSeries = cargarEjerciciosConSeries(sesionId)) }
    }
}

class EntrenoViewModelFactory(
    private val context: Context,
    private val sesionDao: SesionDao,
    private val ejercicioEnSesionDao: EjercicioEnSesionDao,
    private val serieDao: SerieDao,
    private val ejercicioDao: EjercicioDao,
    private val tipoSesionDao: TipoSesionDao,
    private val generarSesionUseCase: GenerarSesionUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        EntrenoViewModel(
            context, sesionDao, ejercicioEnSesionDao, serieDao,
            ejercicioDao, tipoSesionDao, generarSesionUseCase
        ) as T
}
