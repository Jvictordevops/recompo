package com.vic.recompo.ui.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vic.recompo.data.UserSettingsStore
import com.vic.recompo.domain.model.Sexo
import com.vic.recompo.domain.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class WizardState(
    val step: Int = 0,
    // Paso 1 — datos personales
    val nombre: String = "",
    val fechaNacimiento: String = "",
    val sexo: Sexo? = null,
    val alturaCm: String = "",
    // Paso 2 — plan y macros
    val fechaInicioPlan: String = LocalDate.now().toString(),
    val pesoInicialKg: String = "",
    val pesoObjetivoKg: String = "",
    val faseActual: String = "Fase 1",
    val kcalDescanso: String = "1900",
    val kcalMusculacion: String = "2050",
    val kcalBici: String = "2150",
    val proteinaObjetivoG: String = "160",
    // Paso 3 — backup
    val backupUri: String? = null,
    // Control
    val error: String? = null,
    val isSaving: Boolean = false
)

class WizardViewModel(private val store: UserSettingsStore) : ViewModel() {

    private val _state = MutableStateFlow(WizardState())
    val state: StateFlow<WizardState> = _state.asStateFlow()

    fun onNombreChanged(v: String) = _state.update { it.copy(nombre = v, error = null) }
    fun onFechaNacimientoChanged(v: String) = _state.update { it.copy(fechaNacimiento = v, error = null) }
    fun onSexoChanged(v: Sexo) = _state.update { it.copy(sexo = v, error = null) }
    fun onAlturaCmChanged(v: String) = _state.update { it.copy(alturaCm = v, error = null) }
    fun onFechaInicioPlanChanged(v: String) = _state.update { it.copy(fechaInicioPlan = v, error = null) }
    fun onPesoInicialKgChanged(v: String) = _state.update { it.copy(pesoInicialKg = v, error = null) }
    fun onPesoObjetivoKgChanged(v: String) = _state.update { it.copy(pesoObjetivoKg = v, error = null) }
    fun onFaseActualChanged(v: String) = _state.update { it.copy(faseActual = v, error = null) }
    fun onKcalDescansoChanged(v: String) = _state.update { it.copy(kcalDescanso = v, error = null) }
    fun onKcalMusculacionChanged(v: String) = _state.update { it.copy(kcalMusculacion = v, error = null) }
    fun onKcalBiciChanged(v: String) = _state.update { it.copy(kcalBici = v, error = null) }
    fun onProteinaObjetivoGChanged(v: String) = _state.update { it.copy(proteinaObjetivoG = v, error = null) }
    fun onBackupUriSelected(uri: String) = _state.update { it.copy(backupUri = uri, error = null) }

    fun onNext() {
        val s = _state.value
        when (s.step) {
            0 -> {
                val error = validateStep1(s)
                if (error != null) { _state.update { it.copy(error = error) }; return }
                _state.update { it.copy(step = 1, error = null) }
            }
            1 -> {
                val error = validateStep2(s)
                if (error != null) { _state.update { it.copy(error = error) }; return }
                _state.update { it.copy(step = 2, error = null) }
            }
        }
    }

    fun onBack() = _state.update { if (it.step > 0) it.copy(step = it.step - 1, error = null) else it }

    fun onSaveAndFinish() = saveAndFinish()

    fun onSkipBackup() = saveAndFinish()

    private fun saveAndFinish() {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                store.save(buildUserSettings())
                store.markSetupDone()
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = "Error al guardar: ${e.message}") }
            }
        }
    }

    private fun validateStep1(s: WizardState): String? {
        if (s.nombre.isBlank()) return "El nombre es obligatorio"
        if (runCatching { LocalDate.parse(s.fechaNacimiento) }.isFailure)
            return "Fecha de nacimiento inválida (formato AAAA-MM-DD)"
        if (s.sexo == null) return "Selecciona sexo biológico"
        val altura = s.alturaCm.toIntOrNull()
        if (altura == null || altura < 100 || altura > 250) return "Altura inválida (100–250 cm)"
        return null
    }

    private fun validateStep2(s: WizardState): String? {
        if (runCatching { LocalDate.parse(s.fechaInicioPlan) }.isFailure)
            return "Fecha de inicio inválida (formato AAAA-MM-DD)"
        if (s.pesoInicialKg.toDoubleOrNull()?.let { it > 0 } != true) return "Peso inicial inválido"
        if (s.pesoObjetivoKg.toDoubleOrNull()?.let { it > 0 } != true) return "Peso objetivo inválido"
        if (s.faseActual.isBlank()) return "La fase actual es obligatoria"
        if (s.kcalDescanso.toIntOrNull()?.let { it > 0 } != true) return "Kcal descanso inválido"
        if (s.kcalMusculacion.toIntOrNull()?.let { it > 0 } != true) return "Kcal musculación inválido"
        if (s.kcalBici.toIntOrNull()?.let { it > 0 } != true) return "Kcal bici inválido"
        if (s.proteinaObjetivoG.toIntOrNull()?.let { it > 0 } != true) return "Proteína objetivo inválida"
        return null
    }

    private fun buildUserSettings(): UserSettings {
        val s = _state.value
        return UserSettings(
            nombre = s.nombre,
            fechaNacimiento = LocalDate.parse(s.fechaNacimiento),
            sexo = s.sexo!!,
            alturaCm = s.alturaCm.toInt(),
            fechaInicioPlan = LocalDate.parse(s.fechaInicioPlan),
            pesoInicialKg = s.pesoInicialKg.toDouble(),
            pesoObjetivoKg = s.pesoObjetivoKg.toDouble(),
            faseActual = s.faseActual,
            kcalDescanso = s.kcalDescanso.toInt(),
            kcalMusculacion = s.kcalMusculacion.toInt(),
            kcalBici = s.kcalBici.toInt(),
            proteinaObjetivoG = s.proteinaObjetivoG.toInt(),
            carpetaBackupUri = s.backupUri,
            ultimoBackupOk = null,
            ultimoBackupError = null,
            ultimoBackupBytes = null
        )
    }
}

class WizardViewModelFactory(private val store: UserSettingsStore) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = WizardViewModel(store) as T
}
