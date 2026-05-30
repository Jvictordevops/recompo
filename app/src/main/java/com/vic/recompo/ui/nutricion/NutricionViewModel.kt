package com.vic.recompo.ui.nutricion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vic.recompo.data.ai.ParseComidaUseCase
import com.vic.recompo.data.db.dao.ComidaBaseDao
import com.vic.recompo.data.db.dao.EntradaComidaDao
import com.vic.recompo.data.db.entity.ComidaBase
import com.vic.recompo.data.db.entity.EntradaComida
import com.vic.recompo.domain.model.SlotComida
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

data class TotalesDelDia(
    val kcal: Int = 0,
    val proteinaG: Double = 0.0,
    val grasaG: Double = 0.0,
    val carboG: Double = 0.0
)

data class NutricionUiState(
    val entradasPorSlot: Map<SlotComida, List<EntradaComida>> = emptyMap(),
    val totales: TotalesDelDia = TotalesDelDia(),
    val plantillasPorSlot: Map<SlotComida, List<ComidaBase>> = emptyMap()
)

data class DialogState(
    val abierto: Boolean = false,
    val slot: SlotComida = SlotComida.DESAYUNO,
    val entradaEditando: EntradaComida? = null,
    val modoPlantilla: Boolean = true,
    val plantillaSeleccionada: ComidaBase? = null,
    val textoLibre: String = "",
    val kcal: String = "",
    val proteinaG: String = "",
    val grasaG: String = "",
    val carboG: String = "",
    val parseando: Boolean = false,
    val confianza: String? = null,
    val parseadaPorIA: Boolean = false,
    val errorIA: String? = null
)

class NutricionViewModel(
    private val entradaComidaDao: EntradaComidaDao,
    private val comidaBaseDao: ComidaBaseDao,
    private val parseComidaUseCase: ParseComidaUseCase
) : ViewModel() {

    private val hoy = LocalDate.now()

    private val _dialogState = MutableStateFlow(DialogState())
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    val uiState: StateFlow<NutricionUiState> = combine(
        entradaComidaDao.getByFecha(hoy),
        comidaBaseDao.getAllActivos()
    ) { entradas, plantillas ->
        NutricionUiState(
            entradasPorSlot = entradas.groupBy { it.slot },
            totales = TotalesDelDia(
                kcal = entradas.sumOf { it.kcal },
                proteinaG = entradas.sumOf { it.proteinaG },
                grasaG = entradas.sumOf { it.grasaG },
                carboG = entradas.sumOf { it.carboG }
            ),
            plantillasPorSlot = plantillas.groupBy { it.slot }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NutricionUiState())

    fun abrirDialogo(slot: SlotComida, entrada: EntradaComida? = null) {
        _dialogState.value = if (entrada != null) {
            DialogState(
                abierto = true,
                slot = slot,
                entradaEditando = entrada,
                modoPlantilla = false,
                textoLibre = entrada.textoLibre,
                kcal = entrada.kcal.toString(),
                proteinaG = entrada.proteinaG.toString(),
                grasaG = entrada.grasaG.toString(),
                carboG = entrada.carboG.toString()
            )
        } else {
            DialogState(abierto = true, slot = slot)
        }
    }

    fun cerrarDialogo() { _dialogState.value = DialogState() }

    fun onModoPlantillaChanged(modoPlantilla: Boolean) {
        _dialogState.update {
            it.copy(
                modoPlantilla = modoPlantilla,
                plantillaSeleccionada = null,
                textoLibre = "", kcal = "", proteinaG = "", grasaG = "", carboG = ""
            )
        }
    }

    fun onPlantillaSeleccionada(plantilla: ComidaBase) {
        _dialogState.update {
            it.copy(
                plantillaSeleccionada = plantilla,
                textoLibre = plantilla.variante,
                kcal = plantilla.kcal.toString(),
                proteinaG = plantilla.proteinaG.toString(),
                grasaG = plantilla.grasaG.toString(),
                carboG = plantilla.carboG.toString()
            )
        }
    }

    fun onTextoLibreChanged(v: String) = _dialogState.update { it.copy(textoLibre = v, confianza = null, parseadaPorIA = false, errorIA = null) }
    fun onKcalChanged(v: String) = _dialogState.update { it.copy(kcal = v) }
    fun onProteinaChanged(v: String) = _dialogState.update { it.copy(proteinaG = v) }
    fun onGrasaChanged(v: String) = _dialogState.update { it.copy(grasaG = v) }
    fun onCarboChanged(v: String) = _dialogState.update { it.copy(carboG = v) }

    fun parsearConIA() {
        val texto = _dialogState.value.textoLibre.ifBlank { return }
        _dialogState.update { it.copy(parseando = true) }
        viewModelScope.launch {
            parseComidaUseCase.parsear(texto).onSuccess { parsed ->
                _dialogState.update {
                    it.copy(
                        parseando = false,
                        kcal = parsed.kcal.toString(),
                        proteinaG = parsed.proteinaG.toString(),
                        grasaG = parsed.grasaG.toString(),
                        carboG = parsed.carboG.toString(),
                        confianza = parsed.confianza,
                        parseadaPorIA = true
                    )
                }
            }.onFailure { e ->
                android.util.Log.e("ParseComida", "ViewModel onFailure: ${e::class.simpleName}: ${e.message}")
                _dialogState.update { it.copy(parseando = false, errorIA = "${e::class.simpleName}: ${e.message}") }
            }
        }
    }

    fun guardar() {
        val d = _dialogState.value
        val kcal = d.kcal.toIntOrNull() ?: return
        val prot = d.proteinaG.toDoubleOrNull() ?: return
        val grasa = d.grasaG.toDoubleOrNull() ?: return
        val carbo = d.carboG.toDoubleOrNull() ?: return
        viewModelScope.launch {
            val entrada = EntradaComida(
                id = d.entradaEditando?.id ?: 0,
                fecha = hoy,
                slot = d.slot,
                textoLibre = d.textoLibre,
                kcal = kcal,
                proteinaG = prot,
                grasaG = grasa,
                carboG = carbo,
                comidaBaseId = d.plantillaSeleccionada?.id,
                parseadaPorIA = d.parseadaPorIA,
                timestamp = Instant.now()
            )
            if (d.entradaEditando != null) entradaComidaDao.update(entrada)
            else entradaComidaDao.insert(entrada)
            cerrarDialogo()
        }
    }

    fun borrar(id: Long) {
        viewModelScope.launch { entradaComidaDao.deleteById(id) }
    }
}

class NutricionViewModelFactory(
    private val entradaComidaDao: EntradaComidaDao,
    private val comidaBaseDao: ComidaBaseDao,
    private val parseComidaUseCase: ParseComidaUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NutricionViewModel(entradaComidaDao, comidaBaseDao, parseComidaUseCase) as T
}
