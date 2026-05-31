package com.vic.recompo.ui.nutricion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vic.recompo.data.ai.DescomponerComidaUseCase
import com.vic.recompo.data.ai.ParseComidaUseCase
import com.vic.recompo.data.db.dao.ComidaBaseDao
import com.vic.recompo.data.db.dao.EntradaComidaDao
import com.vic.recompo.data.db.dao.IngredienteDao
import com.vic.recompo.data.db.dao.PlantillaIngredienteDao
import com.vic.recompo.data.db.entity.ComidaBase
import com.vic.recompo.data.db.entity.EntradaComida
import com.vic.recompo.data.db.entity.Ingrediente
import com.vic.recompo.data.db.entity.PlantillaIngrediente
import com.vic.recompo.domain.ai.ParseComidaResult
import com.vic.recompo.domain.model.Fiabilidad
import com.vic.recompo.domain.model.SlotComida
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
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
    val plantillasPorSlot: Map<SlotComida, List<ComidaBase>> = emptyMap(),
    val idsEscalables: Set<Long> = emptySet()
)

data class ResultadoIAState(
    val kcal: Int,
    val proteinaG: Double,
    val grasaG: Double,
    val carboG: Double,
    val confianza: String,
    val supuestos: String?
)

data class IngredienteEditable(
    val plantillaId: Long,
    val ingredienteId: Long,
    val nombre: String,
    val kcal100g: Double,
    val prot100g: Double,
    val grasa100g: Double,
    val carbo100g: Double,
    val gramosPorUnidad: Int?,
    val nombreUnidad: String?,
    val cantidadG: String
) {
    val cantidadDouble: Double get() = cantidadG.toDoubleOrNull() ?: 0.0
    fun kcalCalculado(): Int = (kcal100g * cantidadDouble / 100).toInt()
    fun protCalculado(): Double = prot100g * cantidadDouble / 100
    fun grasaCalculado(): Double = grasa100g * cantidadDouble / 100
    fun carboCalculado(): Double = carbo100g * cantidadDouble / 100
}

data class IngredienteParaRevisar(
    val nombre: String,
    val cantidadG: String,
    val kcal100g: String,
    val prot100g: String,
    val grasa100g: String,
    val carbo100g: String,
    val esDeCatalogo: Boolean,
    val ingredienteExistenteId: Long? = null
) {
    fun kcalCalculado(): Int {
        val cant = cantidadG.toDoubleOrNull() ?: 0.0
        val kcal = kcal100g.toDoubleOrNull() ?: 0.0
        return (kcal * cant / 100).toInt()
    }
}

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
    val parseadaPorIA: Boolean = false,
    val errorIA: String? = null,
    val preguntasIA: List<String>? = null,
    val respuestaAclaracion: String = "",
    val resultadoIA: ResultadoIAState? = null,
    val afinando: Boolean = false,
    val textoAfinar: String = "",
    val sugerencias: List<EntradaComida> = emptyList(),
    val plantillasSugeridas: List<ComidaBase> = emptyList(),
    val ingredientesEditables: List<IngredienteEditable> = emptyList(),
    // Flujo descomposición
    val descomponiendo: Boolean = false,
    val errorDescomposicion: String? = null,
    val dialogoRevisarDescomposicion: Boolean = false,
    val ingredientesParaRevisar: List<IngredienteParaRevisar> = emptyList(),
    val nombrePlantillaDescompuesta: String = "",
    val slotPlantillaDescompuesta: SlotComida = SlotComida.DESAYUNO,
    val guardandoDescomposicion: Boolean = false,
    // Gestor de plantillas
    val gestorPlantillasAbierto: Boolean = false
)

class NutricionViewModel(
    private val entradaComidaDao: EntradaComidaDao,
    private val comidaBaseDao: ComidaBaseDao,
    private val plantillaIngredienteDao: PlantillaIngredienteDao,
    private val ingredienteDao: IngredienteDao,
    private val parseComidaUseCase: ParseComidaUseCase,
    private val descomponerComidaUseCase: DescomponerComidaUseCase
) : ViewModel() {

    private val hoy = LocalDate.now()
    private val _dialogState = MutableStateFlow(DialogState())
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()
    private var sugerenciasJob: Job? = null
    private val _idsEscalables = MutableStateFlow<Set<Long>>(emptySet())

    init {
        viewModelScope.launch {
            _idsEscalables.value = plantillaIngredienteDao.getAllComidaBaseIdsConIngredientes().toSet()
        }
    }

    val uiState: StateFlow<NutricionUiState> = combine(
        entradaComidaDao.getByFecha(hoy),
        comidaBaseDao.getAllActivos(),
        _idsEscalables
    ) { entradas, plantillas, idsEscalables ->
        NutricionUiState(
            entradasPorSlot = entradas.groupBy { it.slot },
            totales = TotalesDelDia(
                kcal = entradas.sumOf { it.kcal },
                proteinaG = entradas.sumOf { it.proteinaG },
                grasaG = entradas.sumOf { it.grasaG },
                carboG = entradas.sumOf { it.carboG }
            ),
            plantillasPorSlot = plantillas.groupBy { it.slot },
            idsEscalables = idsEscalables
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

    fun cerrarDialogo() {
        sugerenciasJob?.cancel()
        _dialogState.value = DialogState()
    }

    fun onModoPlantillaChanged(modoPlantilla: Boolean) {
        sugerenciasJob?.cancel()
        _dialogState.update {
            it.copy(
                modoPlantilla = modoPlantilla,
                plantillaSeleccionada = null,
                textoLibre = "", kcal = "", proteinaG = "", grasaG = "", carboG = "",
                preguntasIA = null, resultadoIA = null, errorIA = null,
                sugerencias = emptyList(), plantillasSugeridas = emptyList(),
                ingredientesEditables = emptyList()
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
                carboG = plantilla.carboG.toString(),
                ingredientesEditables = emptyList(),
                sugerencias = emptyList(),
                plantillasSugeridas = emptyList()
            )
        }
        viewModelScope.launch {
            val items = plantillaIngredienteDao.getByComidaBase(plantilla.id)
            if (items.isNotEmpty()) {
                val editables = items.map { pic ->
                    val cant = pic.plantilla.cantidadG
                    IngredienteEditable(
                        plantillaId = pic.plantilla.id,
                        ingredienteId = pic.ingrediente.id,
                        nombre = pic.ingrediente.nombre,
                        kcal100g = pic.ingrediente.kcal100g,
                        prot100g = pic.ingrediente.prot100g,
                        grasa100g = pic.ingrediente.grasa100g,
                        carbo100g = pic.ingrediente.carbo100g,
                        gramosPorUnidad = pic.ingrediente.gramosPorUnidad,
                        nombreUnidad = pic.ingrediente.nombreUnidad,
                        cantidadG = if (cant == cant.toLong().toDouble()) cant.toLong().toString() else cant.toString()
                    )
                }
                val totales = calcularTotales(editables)
                _dialogState.update {
                    it.copy(
                        ingredientesEditables = editables,
                        kcal = totales.kcal.toString(),
                        proteinaG = "%.1f".format(totales.prot),
                        grasaG = "%.1f".format(totales.grasa),
                        carboG = "%.1f".format(totales.carbo)
                    )
                }
            }
        }
    }

    fun onTextoLibreChanged(v: String) {
        _dialogState.update { it.copy(textoLibre = v, preguntasIA = null, resultadoIA = null, errorIA = null, parseadaPorIA = false) }
        actualizarSugerencias(v)
    }

    private fun actualizarSugerencias(query: String) {
        sugerenciasJob?.cancel()
        if (query.length < 2) {
            _dialogState.update { it.copy(sugerencias = emptyList(), plantillasSugeridas = emptyList()) }
            return
        }
        sugerenciasJob = viewModelScope.launch {
            delay(300)
            val plantillas = comidaBaseDao.searchByVariante(query)
            val plantillasNormalizadas = plantillas.map { normalizar(it.variante) }.toSet()
            val historico = entradaComidaDao.search(query)
                .distinctBy { normalizar(it.textoLibre) }
                .filter { normalizar(it.textoLibre) !in plantillasNormalizadas }
                .take(5)
            _dialogState.update { it.copy(plantillasSugeridas = plantillas, sugerencias = historico) }
        }
    }

    fun seleccionarSugerencia(entrada: EntradaComida) {
        sugerenciasJob?.cancel()
        _dialogState.update {
            it.copy(
                textoLibre = entrada.textoLibre,
                kcal = entrada.kcal.toString(),
                proteinaG = entrada.proteinaG.toString(),
                grasaG = entrada.grasaG.toString(),
                carboG = entrada.carboG.toString(),
                parseadaPorIA = entrada.parseadaPorIA,
                sugerencias = emptyList(),
                plantillasSugeridas = emptyList()
            )
        }
    }

    fun seleccionarSugerenciaPlantilla(plantilla: ComidaBase) {
        sugerenciasJob?.cancel()
        _dialogState.update { it.copy(modoPlantilla = true, sugerencias = emptyList(), plantillasSugeridas = emptyList()) }
        onPlantillaSeleccionada(plantilla)
    }

    fun cerrarSugerencias() {
        sugerenciasJob?.cancel()
        _dialogState.update { it.copy(sugerencias = emptyList(), plantillasSugeridas = emptyList()) }
    }

    fun onCantidadIngredienteChanged(index: Int, value: String) {
        val current = _dialogState.value.ingredientesEditables.toMutableList()
        if (index !in current.indices) return
        current[index] = current[index].copy(cantidadG = value)
        val totales = calcularTotales(current)
        _dialogState.update {
            it.copy(
                ingredientesEditables = current,
                kcal = totales.kcal.toString(),
                proteinaG = "%.1f".format(totales.prot),
                grasaG = "%.1f".format(totales.grasa),
                carboG = "%.1f".format(totales.carbo)
            )
        }
    }

    fun onKcalChanged(v: String) = _dialogState.update { it.copy(kcal = v) }
    fun onProteinaChanged(v: String) = _dialogState.update { it.copy(proteinaG = v) }
    fun onGrasaChanged(v: String) = _dialogState.update { it.copy(grasaG = v) }
    fun onCarboChanged(v: String) = _dialogState.update { it.copy(carboG = v) }

    fun onRespuestaAclaracionChanged(v: String) = _dialogState.update { it.copy(respuestaAclaracion = v) }
    fun onTextoAfinarChanged(v: String) = _dialogState.update { it.copy(textoAfinar = v) }
    fun abrirAfinar() = _dialogState.update { it.copy(afinando = true, textoAfinar = "", errorIA = null) }

    // ── Descomposición para guardar plantilla escalable ──────────────────────

    fun mostrarGuardarPlantilla() {
        iniciarDescomposicion()
    }

    fun iniciarDescomposicion() {
        val texto = _dialogState.value.textoLibre.ifBlank { return }
        val slot = _dialogState.value.slot
        _dialogState.update {
            it.copy(
                descomponiendo = true,
                errorDescomposicion = null,
                dialogoRevisarDescomposicion = false,
                ingredientesParaRevisar = emptyList(),
                nombrePlantillaDescompuesta = texto,
                slotPlantillaDescompuesta = slot
            )
        }
        viewModelScope.launch {
            descomponerComidaUseCase.descomponer(texto)
                .onSuccess { lista ->
                    val revisables = lista.map { ing ->
                        val match = ingredienteDao.search(normalizar(ing.nombre)).firstOrNull()
                        if (match != null) {
                            IngredienteParaRevisar(
                                nombre = match.nombre,
                                cantidadG = formatGramos(ing.cantidadG),
                                kcal100g = "%.1f".format(match.kcal100g),
                                prot100g = "%.1f".format(match.prot100g),
                                grasa100g = "%.1f".format(match.grasa100g),
                                carbo100g = "%.1f".format(match.carbo100g),
                                esDeCatalogo = true,
                                ingredienteExistenteId = match.id
                            )
                        } else {
                            IngredienteParaRevisar(
                                nombre = ing.nombre,
                                cantidadG = formatGramos(ing.cantidadG),
                                kcal100g = "%.1f".format(ing.kcal100g),
                                prot100g = "%.1f".format(ing.prot100g),
                                grasa100g = "%.1f".format(ing.grasa100g),
                                carbo100g = "%.1f".format(ing.carbo100g),
                                esDeCatalogo = false,
                                ingredienteExistenteId = null
                            )
                        }
                    }
                    _dialogState.update {
                        it.copy(
                            descomponiendo = false,
                            ingredientesParaRevisar = revisables,
                            dialogoRevisarDescomposicion = true
                        )
                    }
                }
                .onFailure { e ->
                    Timber.w(e, "DescomponerComida failed")
                    _dialogState.update {
                        it.copy(
                            descomponiendo = false,
                            errorDescomposicion = "No se pudo descomponer: ${e.message}"
                        )
                    }
                }
        }
    }

    fun onCantidadRevisarChanged(index: Int, value: String) = updateRevisar(index) { copy(cantidadG = value) }
    fun onKcal100gRevisarChanged(index: Int, value: String) = updateRevisar(index) { copy(kcal100g = value) }
    fun onProt100gRevisarChanged(index: Int, value: String) = updateRevisar(index) { copy(prot100g = value) }
    fun onGrasa100gRevisarChanged(index: Int, value: String) = updateRevisar(index) { copy(grasa100g = value) }
    fun onCarbo100gRevisarChanged(index: Int, value: String) = updateRevisar(index) { copy(carbo100g = value) }

    private fun updateRevisar(index: Int, block: IngredienteParaRevisar.() -> IngredienteParaRevisar) {
        val list = _dialogState.value.ingredientesParaRevisar.toMutableList()
        if (index !in list.indices) return
        list[index] = list[index].block()
        _dialogState.update { it.copy(ingredientesParaRevisar = list) }
    }

    fun onEliminarIngredienteRevisar(index: Int) {
        val list = _dialogState.value.ingredientesParaRevisar.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        _dialogState.update { it.copy(ingredientesParaRevisar = list) }
    }

    fun onNombrePlantillaDescompuestaChanged(v: String) = _dialogState.update { it.copy(nombrePlantillaDescompuesta = v) }
    fun onSlotPlantillaDescompuestaChanged(s: SlotComida) = _dialogState.update { it.copy(slotPlantillaDescompuesta = s) }

    fun cancelarDescomposicion() {
        _dialogState.update {
            it.copy(
                dialogoRevisarDescomposicion = false,
                ingredientesParaRevisar = emptyList(),
                errorDescomposicion = null,
                descomponiendo = false,
                guardandoDescomposicion = false
            )
        }
    }

    fun confirmarDescomposicion() {
        val d = _dialogState.value
        val nombre = d.nombrePlantillaDescompuesta.ifBlank { return }
        val items = d.ingredientesParaRevisar.filter { it.cantidadG.toDoubleOrNull() != null }
        if (items.isEmpty()) return
        _dialogState.update { it.copy(guardandoDescomposicion = true) }
        viewModelScope.launch {
            val idsResueltos = items.map { ing ->
                if (ing.ingredienteExistenteId != null) {
                    ing.ingredienteExistenteId
                } else {
                    ingredienteDao.insert(
                        Ingrediente(
                            nombre = ing.nombre,
                            kcal100g = ing.kcal100g.toDoubleOrNull() ?: 0.0,
                            prot100g = ing.prot100g.toDoubleOrNull() ?: 0.0,
                            grasa100g = ing.grasa100g.toDoubleOrNull() ?: 0.0,
                            carbo100g = ing.carbo100g.toDoubleOrNull() ?: 0.0,
                            fiabilidad = Fiabilidad.ESTIMADO,
                            activo = true
                        )
                    )
                }
            }
            val totalKcal = items.zip(idsResueltos).sumOf { (ing, _) ->
                val kcal = ing.kcal100g.toDoubleOrNull() ?: 0.0
                val cant = ing.cantidadG.toDoubleOrNull() ?: 0.0
                (kcal * cant / 100).toInt()
            }
            val totalProt = items.sumOf { (it.prot100g.toDoubleOrNull() ?: 0.0) * (it.cantidadG.toDoubleOrNull() ?: 0.0) / 100 }
            val totalGrasa = items.sumOf { (it.grasa100g.toDoubleOrNull() ?: 0.0) * (it.cantidadG.toDoubleOrNull() ?: 0.0) / 100 }
            val totalCarbo = items.sumOf { (it.carbo100g.toDoubleOrNull() ?: 0.0) * (it.cantidadG.toDoubleOrNull() ?: 0.0) / 100 }

            val cbId = comidaBaseDao.insert(
                ComidaBase(
                    slot = d.slotPlantillaDescompuesta,
                    variante = nombre,
                    kcal = totalKcal,
                    proteinaG = totalProt,
                    grasaG = totalGrasa,
                    carboG = totalCarbo,
                    ingredientesTexto = d.textoLibre,
                    activo = true
                )
            )
            items.zip(idsResueltos).forEach { (ing, ingId) ->
                plantillaIngredienteDao.insert(
                    PlantillaIngrediente(
                        comidaBaseId = cbId,
                        ingredienteId = ingId,
                        cantidadG = ing.cantidadG.toDoubleOrNull() ?: 0.0
                    )
                )
            }
            _idsEscalables.value = plantillaIngredienteDao.getAllComidaBaseIdsConIngredientes().toSet()
            cerrarDialogo()
        }
    }

    fun fallbackGuardarComoFoto() {
        val d = _dialogState.value
        val nombre = d.nombrePlantillaDescompuesta.ifBlank { d.textoLibre.ifBlank { return } }
        val kcal = d.resultadoIA?.kcal ?: d.kcal.toIntOrNull() ?: return
        val prot = d.resultadoIA?.proteinaG ?: d.proteinaG.toDoubleOrNull() ?: return
        val grasa = d.resultadoIA?.grasaG ?: d.grasaG.toDoubleOrNull() ?: return
        val carbo = d.resultadoIA?.carboG ?: d.carboG.toDoubleOrNull() ?: return
        viewModelScope.launch {
            comidaBaseDao.insert(
                ComidaBase(
                    slot = d.slotPlantillaDescompuesta.takeIf { d.nombrePlantillaDescompuesta.isNotBlank() } ?: d.slot,
                    variante = nombre,
                    kcal = kcal,
                    proteinaG = prot,
                    grasaG = grasa,
                    carboG = carbo,
                    ingredientesTexto = d.textoLibre,
                    activo = true
                )
            )
            cancelarDescomposicion()
        }
    }

    fun abrirGestorPlantillas() {
        _dialogState.update { it.copy(gestorPlantillasAbierto = true) }
    }

    fun cerrarGestorPlantillas() {
        _dialogState.update { it.copy(gestorPlantillasAbierto = false) }
    }

    fun eliminarPlantilla(id: Long) {
        viewModelScope.launch {
            comidaBaseDao.deleteById(id)
            _idsEscalables.value = plantillaIngredienteDao.getAllComidaBaseIdsConIngredientes().toSet()
        }
    }

    // ── Parseo IA ─────────────────────────────────────────────────────────────

    fun parsearConIA() {
        val texto = _dialogState.value.textoLibre.ifBlank { return }
        _dialogState.update { it.copy(parseando = true, errorIA = null, preguntasIA = null, resultadoIA = null) }
        viewModelScope.launch {
            parseComidaUseCase.parsear(texto)
                .onSuccess { handleResultado(it) }
                .onFailure { e -> onErrorIA(e) }
        }
    }

    fun enviarAclaracion() {
        val d = _dialogState.value
        val aclaracion = d.respuestaAclaracion.ifBlank { return }
        _dialogState.update { it.copy(parseando = true, errorIA = null) }
        viewModelScope.launch {
            parseComidaUseCase.parsearConContexto(d.textoLibre, aclaracion, forzarCalculo = true)
                .onSuccess { handleResultado(it, forced = true) }
                .onFailure { e -> onErrorIA(e) }
        }
    }

    fun enviarAfinar() {
        val d = _dialogState.value
        val afinar = d.textoAfinar.ifBlank { return }
        _dialogState.update { it.copy(parseando = true, errorIA = null) }
        viewModelScope.launch {
            parseComidaUseCase.parsearConContexto(d.textoLibre, afinar, forzarCalculo = true)
                .onSuccess { handleResultado(it, forced = true) }
                .onFailure { e -> onErrorIA(e) }
        }
    }

    private fun handleResultado(resultado: ParseComidaResult, forced: Boolean = false) {
        when (resultado) {
            is ParseComidaResult.NecesitaAclaracion -> {
                if (forced) {
                    _dialogState.update {
                        it.copy(parseando = false, errorIA = "La IA no pudo calcular los macros. Introduce los valores a mano.")
                    }
                } else {
                    _dialogState.update {
                        it.copy(parseando = false, preguntasIA = resultado.preguntas, resultadoIA = null, respuestaAclaracion = "")
                    }
                }
            }
            is ParseComidaResult.Calculado -> _dialogState.update {
                it.copy(
                    parseando = false,
                    preguntasIA = null,
                    afinando = false,
                    textoAfinar = "",
                    resultadoIA = ResultadoIAState(
                        kcal = resultado.kcal,
                        proteinaG = resultado.proteinaG,
                        grasaG = resultado.grasaG,
                        carboG = resultado.carboG,
                        confianza = resultado.confianza,
                        supuestos = resultado.supuestos
                    )
                )
            }
        }
    }

    private fun onErrorIA(e: Throwable) {
        android.util.Log.e("ParseComida", "ViewModel onFailure: ${e.message}")
        _dialogState.update { it.copy(parseando = false, errorIA = "${e::class.simpleName}: ${e.message}") }
    }

    fun aceptarYGuardar() {
        val d = _dialogState.value
        val resultado = d.resultadoIA ?: return
        viewModelScope.launch {
            val entrada = EntradaComida(
                id = d.entradaEditando?.id ?: 0,
                fecha = hoy,
                slot = d.slot,
                textoLibre = d.textoLibre,
                kcal = resultado.kcal,
                proteinaG = resultado.proteinaG,
                grasaG = resultado.grasaG,
                carboG = resultado.carboG,
                comidaBaseId = null,
                parseadaPorIA = true,
                timestamp = Instant.now()
            )
            if (d.entradaEditando != null) entradaComidaDao.update(entrada)
            else entradaComidaDao.insert(entrada)
            cerrarDialogo()
        }
    }

    fun editarAMano() {
        val resultado = _dialogState.value.resultadoIA ?: return
        _dialogState.update {
            it.copy(
                resultadoIA = null,
                preguntasIA = null,
                afinando = false,
                kcal = resultado.kcal.toString(),
                proteinaG = resultado.proteinaG.toString(),
                grasaG = resultado.grasaG.toString(),
                carboG = resultado.carboG.toString(),
                parseadaPorIA = true
            )
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

    // ── Utilidades ────────────────────────────────────────────────────────────

    private data class TotalesCalc(val kcal: Int, val prot: Double, val grasa: Double, val carbo: Double)

    private fun calcularTotales(editables: List<IngredienteEditable>): TotalesCalc = TotalesCalc(
        kcal = editables.sumOf { it.kcalCalculado() },
        prot = editables.sumOf { it.protCalculado() },
        grasa = editables.sumOf { it.grasaCalculado() },
        carbo = editables.sumOf { it.carboCalculado() }
    )

    private fun formatGramos(v: Double): String =
        if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)

    private fun normalizar(text: String): String = text
        .lowercase()
        .map { c ->
            when (c) {
                'á', 'à', 'â', 'ä' -> 'a'
                'é', 'è', 'ê', 'ë' -> 'e'
                'í', 'ì', 'î', 'ï' -> 'i'
                'ó', 'ò', 'ô', 'ö' -> 'o'
                'ú', 'ù', 'û', 'ü' -> 'u'
                'ñ' -> 'n'
                else -> c
            }
        }
        .joinToString("")
        .replace(Regex("[^a-z0-9]"), "")
}

class NutricionViewModelFactory(
    private val entradaComidaDao: EntradaComidaDao,
    private val comidaBaseDao: ComidaBaseDao,
    private val plantillaIngredienteDao: PlantillaIngredienteDao,
    private val ingredienteDao: IngredienteDao,
    private val parseComidaUseCase: ParseComidaUseCase,
    private val descomponerComidaUseCase: DescomponerComidaUseCase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NutricionViewModel(
            entradaComidaDao, comidaBaseDao, plantillaIngredienteDao,
            ingredienteDao, parseComidaUseCase, descomponerComidaUseCase
        ) as T
}
