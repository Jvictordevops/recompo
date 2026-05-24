package com.vic.recompo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vic.recompo.data.UserSettingsStore
import com.vic.recompo.data.db.dao.ActividadDao
import com.vic.recompo.data.db.dao.EntradaComidaDao
import com.vic.recompo.data.db.dao.SesionDao
import com.vic.recompo.data.db.entity.Sesion
import com.vic.recompo.domain.model.EstadoSesion
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate

enum class TipoDia { DESCANSO, MUSCULACION, BICI }

data class HomeUiState(
    val nombre: String = "",
    val tipoDia: TipoDia = TipoDia.DESCANSO,
    val kcalConsumidas: Int = 0,
    val kcalObjetivo: Int = 0,
    val proteinaConsumidaG: Double = 0.0,
    val proteinaObjetivoG: Int = 0,
    val sesionDelDia: Sesion? = null,
    val backupUri: String? = null,
    val ultimoBackupOk: Instant? = null,
    val ultimoBackupError: String? = null
)

class HomeViewModel(
    settingsStore: UserSettingsStore,
    entradaComidaDao: EntradaComidaDao,
    sesionDao: SesionDao,
    actividadDao: ActividadDao
) : ViewModel() {

    private val hoy = LocalDate.now()

    val uiState: StateFlow<HomeUiState> = combine(
        settingsStore.settings,
        entradaComidaDao.getByFecha(hoy),
        sesionDao.getByFecha(hoy),
        actividadDao.getByFecha(hoy)
    ) { settings, entradas, sesiones, actividades ->
        val sesionActiva = sesiones.firstOrNull {
            it.estado == EstadoSesion.PLANIFICADA || it.estado == EstadoSesion.EN_CURSO
        }
        val tipoDia = when {
            sesionActiva != null -> TipoDia.MUSCULACION
            actividades.any { it.tipo.lowercase() == "bici" } -> TipoDia.BICI
            else -> TipoDia.DESCANSO
        }
        val kcalObjetivo = when (tipoDia) {
            TipoDia.MUSCULACION -> settings?.kcalMusculacion ?: 0
            TipoDia.BICI -> settings?.kcalBici ?: 0
            TipoDia.DESCANSO -> settings?.kcalDescanso ?: 0
        }
        HomeUiState(
            nombre = settings?.nombre ?: "",
            tipoDia = tipoDia,
            kcalConsumidas = entradas.sumOf { it.kcal },
            kcalObjetivo = kcalObjetivo,
            proteinaConsumidaG = entradas.sumOf { it.proteinaG },
            proteinaObjetivoG = settings?.proteinaObjetivoG ?: 0,
            sesionDelDia = sesionActiva,
            backupUri = settings?.carpetaBackupUri,
            ultimoBackupOk = settings?.ultimoBackupOk,
            ultimoBackupError = settings?.ultimoBackupError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}

class HomeViewModelFactory(
    private val settingsStore: UserSettingsStore,
    private val entradaComidaDao: EntradaComidaDao,
    private val sesionDao: SesionDao,
    private val actividadDao: ActividadDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(settingsStore, entradaComidaDao, sesionDao, actividadDao) as T
}
