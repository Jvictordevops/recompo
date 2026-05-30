package com.vic.recompo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vic.recompo.data.UserSettingsStore
import com.vic.recompo.data.db.dao.ActividadDao
import com.vic.recompo.data.db.dao.EntradaComidaDao
import com.vic.recompo.data.db.dao.SesionDao
import com.vic.recompo.data.db.dao.TipoSesionDao
import com.vic.recompo.data.db.entity.Sesion
import com.vic.recompo.domain.model.EstadoSesion
import com.vic.recompo.domain.usecase.CalcularKcalObjetivoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
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
    val metabolismoBasalKcal: Int = 0,
    val kcalActividad: Int = 0,
    val kcalGastoReal: Int = 0,
    val sesionActiva: Sesion? = null,
    val sesionActivaTipoNombre: String? = null,
    val backupUri: String? = null,
    val ultimoBackupOk: Instant? = null,
    val ultimoBackupError: String? = null
)

class HomeViewModel(
    settingsStore: UserSettingsStore,
    entradaComidaDao: EntradaComidaDao,
    sesionDao: SesionDao,
    actividadDao: ActividadDao,
    private val tipoSesionDao: TipoSesionDao
) : ViewModel() {

    private val hoy = LocalDate.now()
    private val calcularKcal = CalcularKcalObjetivoUseCase()
    private val _tipoDiaOverride = MutableStateFlow<TipoDia?>(null)

    fun setTipoDia(tipo: TipoDia) { _tipoDiaOverride.value = tipo }

    val uiState: StateFlow<HomeUiState> = combine(
        settingsStore.settings,
        entradaComidaDao.getByFecha(hoy),
        sesionDao.getActivas(),
        actividadDao.getByFecha(hoy),
        _tipoDiaOverride
    ) { settings, entradas, sesiones, actividades, override ->
        val sesionActiva = sesiones.firstOrNull()
        val tipoNombre = sesionActiva?.let { tipoSesionDao.getById(it.tipoSesionId)?.nombre }
        val tieneBici = actividades.any { it.tipo.lowercase() == "bici" }
        val tipoDiaAuto = when {
            tieneBici -> TipoDia.BICI
            sesionActiva != null -> TipoDia.MUSCULACION
            else -> TipoDia.DESCANSO
        }
        val tipoDia = override ?: tipoDiaAuto
        val kcalObjetivo = calcularKcal.calcularObjetivoNutricional(
            tipoDia = tipoDia,
            kcalDescanso = settings?.kcalDescanso ?: 0,
            kcalMusculacion = settings?.kcalMusculacion ?: 0,
            kcalBici = settings?.kcalBici ?: 0
        )
        val metabolismo = settings?.metabolismoBasalKcal ?: 0
        val kcalActividad = actividades.sumOf { it.kcalQuemadas }
        HomeUiState(
            nombre = settings?.nombre ?: "",
            tipoDia = tipoDia,
            kcalConsumidas = entradas.sumOf { it.kcal },
            kcalObjetivo = kcalObjetivo,
            proteinaConsumidaG = entradas.sumOf { it.proteinaG },
            proteinaObjetivoG = settings?.proteinaObjetivoG ?: 0,
            metabolismoBasalKcal = metabolismo,
            kcalActividad = kcalActividad,
            kcalGastoReal = calcularKcal.calcularGastoReal(metabolismo, actividades),
            sesionActiva = sesionActiva,
            sesionActivaTipoNombre = tipoNombre,
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
    private val actividadDao: ActividadDao,
    private val tipoSesionDao: TipoSesionDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        HomeViewModel(settingsStore, entradaComidaDao, sesionDao, actividadDao, tipoSesionDao) as T
}
