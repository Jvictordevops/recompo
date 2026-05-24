package com.vic.recompo.ui.actividad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vic.recompo.data.db.dao.ActividadDao
import com.vic.recompo.data.db.entity.Actividad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ActividadFormState(
    val id: Long? = null,
    val fecha: LocalDate = LocalDate.now(),
    val tipo: String = "",
    val descripcion: String = "",
    val duracionMin: String = "",
    val kcalQuemadas: String = ""
)

data class ActividadUiState(
    val actividades: List<Actividad> = emptyList(),
    val form: ActividadFormState? = null
)

class ActividadViewModel(private val dao: ActividadDao) : ViewModel() {

    private val _formState = MutableStateFlow<ActividadFormState?>(null)

    val uiState: StateFlow<ActividadUiState> = kotlinx.coroutines.flow.combine(
        dao.getAll(),
        _formState
    ) { actividades, form ->
        ActividadUiState(actividades = actividades, form = form)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ActividadUiState())

    fun abrirNueva() {
        _formState.value = ActividadFormState(fecha = LocalDate.now())
    }

    fun abrirEditar(actividad: Actividad) {
        _formState.value = ActividadFormState(
            id = actividad.id,
            fecha = actividad.fecha,
            tipo = actividad.tipo,
            descripcion = actividad.descripcion ?: "",
            duracionMin = actividad.duracionMin?.toString() ?: "",
            kcalQuemadas = actividad.kcalQuemadas.toString()
        )
    }

    fun cerrarDialog() { _formState.value = null }

    fun onTipoChanged(v: String) = _formState.update { it?.copy(tipo = v) }
    fun onDescripcionChanged(v: String) = _formState.update { it?.copy(descripcion = v) }
    fun onDuracionMinChanged(v: String) = _formState.update { it?.copy(duracionMin = v) }
    fun onKcalQuemadasChanged(v: String) = _formState.update { it?.copy(kcalQuemadas = v) }
    fun onFechaChanged(v: LocalDate) = _formState.update { it?.copy(fecha = v) }

    fun guardar() {
        val f = _formState.value ?: return
        val kcal = f.kcalQuemadas.toIntOrNull() ?: return
        if (f.tipo.isBlank()) return
        viewModelScope.launch {
            val entidad = Actividad(
                id = f.id ?: 0,
                fecha = f.fecha,
                tipo = f.tipo.trim(),
                descripcion = f.descripcion.trim().takeIf { it.isNotEmpty() },
                duracionMin = f.duracionMin.toIntOrNull(),
                kcalQuemadas = kcal
            )
            if (f.id == null) dao.insert(entidad) else dao.update(entidad)
            _formState.value = null
        }
    }

    fun borrar(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }
}

class ActividadViewModelFactory(private val dao: ActividadDao) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ActividadViewModel(dao) as T
}
