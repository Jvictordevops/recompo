package com.vic.recompo.ui.mediciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vic.recompo.data.UserSettingsStore
import com.vic.recompo.data.db.dao.MedicionDao
import com.vic.recompo.data.db.entity.Medicion
import com.vic.recompo.domain.calc.calcularGrasaNavy
import com.vic.recompo.domain.calc.calcularImc
import com.vic.recompo.domain.calc.calcularMasaGrasaKg
import com.vic.recompo.domain.calc.calcularMasaMagraKg
import com.vic.recompo.domain.calc.calcularWhr
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MedicionesUiState(
    val mediciones: List<Medicion> = emptyList()
)

data class MedicionFormState(
    val abierto: Boolean = false,
    val medicionEditando: Medicion? = null,
    val fecha: String = LocalDate.now().toString(),
    val pesoKg: String = "",
    val cinturaCm: String = "",
    val caderaCm: String = "",
    val cuelloCm: String = "",
    val pechoCm: String = "",
    val bicepsCm: String = "",
    val musloCm: String = "",
    val grasaPctOverride: Boolean = false,
    val grasaPctManual: String = "",
    val hito: String = "",
    val notas: String = ""
)

class MedicionesViewModel(
    private val medicionDao: MedicionDao,
    private val userSettingsStore: UserSettingsStore
) : ViewModel() {

    private val _formState = MutableStateFlow(MedicionFormState())
    val formState: StateFlow<MedicionFormState> = _formState.asStateFlow()

    val uiState: StateFlow<MedicionesUiState> = medicionDao.getAll()
        .map { MedicionesUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MedicionesUiState())

    fun abrirFormulario(medicion: Medicion? = null) {
        _formState.value = if (medicion != null) {
            MedicionFormState(
                abierto = true,
                medicionEditando = medicion,
                fecha = medicion.fecha.toString(),
                pesoKg = medicion.pesoKg.toString(),
                cinturaCm = medicion.cinturaCm?.toString() ?: "",
                caderaCm = medicion.caderaCm?.toString() ?: "",
                cuelloCm = medicion.cuelloCm?.toString() ?: "",
                pechoCm = medicion.pechoCm?.toString() ?: "",
                bicepsCm = medicion.bicepsCm?.toString() ?: "",
                musloCm = medicion.musloCm?.toString() ?: "",
                grasaPctOverride = medicion.grasaPctOverride,
                grasaPctManual = if (medicion.grasaPctOverride) medicion.grasaPct?.toString() ?: "" else "",
                hito = medicion.hito ?: "",
                notas = medicion.notas ?: ""
            )
        } else {
            MedicionFormState(abierto = true)
        }
    }

    fun cerrarFormulario() { _formState.value = MedicionFormState() }

    fun onFechaChanged(v: String) = _formState.update { it.copy(fecha = v) }
    fun onPesoChanged(v: String) = _formState.update { it.copy(pesoKg = v) }
    fun onCinturaChanged(v: String) = _formState.update { it.copy(cinturaCm = v) }
    fun onCaderaChanged(v: String) = _formState.update { it.copy(caderaCm = v) }
    fun onCuelloChanged(v: String) = _formState.update { it.copy(cuelloCm = v) }
    fun onPechoChanged(v: String) = _formState.update { it.copy(pechoCm = v) }
    fun onBicepsChanged(v: String) = _formState.update { it.copy(bicepsCm = v) }
    fun onMusloChanged(v: String) = _formState.update { it.copy(musloCm = v) }
    fun onGrasaOverrideChanged(v: Boolean) = _formState.update { it.copy(grasaPctOverride = v, grasaPctManual = "") }
    fun onGrasaManualChanged(v: String) = _formState.update { it.copy(grasaPctManual = v) }
    fun onHitoChanged(v: String) = _formState.update { it.copy(hito = v) }
    fun onNotasChanged(v: String) = _formState.update { it.copy(notas = v) }

    fun guardar() {
        val f = _formState.value
        val peso = f.pesoKg.toDoubleOrNull() ?: return
        val fecha = runCatching { LocalDate.parse(f.fecha) }.getOrNull() ?: return

        viewModelScope.launch {
            val s = userSettingsStore.settings.first() ?: return@launch
            val cintura = f.cinturaCm.toDoubleOrNull()
            val cadera = f.caderaCm.toDoubleOrNull()
            val cuello = f.cuelloCm.toDoubleOrNull()

            // Snapshot de altura al momento de la medición (no cambia en ediciones futuras)
            val alturaCmSnapshot = f.medicionEditando?.alturaCmEnLaMedicion ?: s.alturaCm

            val grasaPct = if (f.grasaPctOverride) {
                f.grasaPctManual.toDoubleOrNull()
            } else {
                calcularGrasaNavy(s.sexo, alturaCmSnapshot, cintura, cuello, cadera)
            }
            val masaGrasaKg = grasaPct?.let { calcularMasaGrasaKg(peso, it) }
            val masaMagraKg = masaGrasaKg?.let { calcularMasaMagraKg(peso, it) }

            val medicion = Medicion(
                id = f.medicionEditando?.id ?: 0,
                fecha = fecha,
                pesoKg = peso,
                cinturaCm = cintura,
                caderaCm = cadera,
                cuelloCm = cuello,
                pechoCm = f.pechoCm.toDoubleOrNull(),
                bicepsCm = f.bicepsCm.toDoubleOrNull(),
                musloCm = f.musloCm.toDoubleOrNull(),
                alturaCmEnLaMedicion = alturaCmSnapshot,
                grasaPct = grasaPct,
                grasaPctOverride = f.grasaPctOverride,
                masaGrasaKg = masaGrasaKg,
                masaMagraKg = masaMagraKg,
                imc = calcularImc(peso, alturaCmSnapshot),
                whr = calcularWhr(cintura, cadera),
                faseTexto = f.medicionEditando?.faseTexto ?: s.faseActual.ifBlank { null },
                hito = f.hito.ifBlank { null },
                notas = f.notas.ifBlank { null }
            )

            if (f.medicionEditando != null) medicionDao.update(medicion)
            else medicionDao.insert(medicion)
            cerrarFormulario()
        }
    }

    fun borrar(id: Long) {
        viewModelScope.launch { medicionDao.deleteById(id) }
    }
}

class MedicionesViewModelFactory(
    private val medicionDao: MedicionDao,
    private val userSettingsStore: UserSettingsStore
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MedicionesViewModel(medicionDao, userSettingsStore) as T
}
