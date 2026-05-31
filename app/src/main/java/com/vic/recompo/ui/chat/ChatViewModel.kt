package com.vic.recompo.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vic.recompo.data.UserSettingsStore
import com.vic.recompo.data.ai.dto.Message
import com.vic.recompo.data.db.dao.ActividadDao
import com.vic.recompo.data.db.dao.ConversacionDao
import com.vic.recompo.data.db.dao.EntradaComidaDao
import com.vic.recompo.data.db.dao.MedicionDao
import com.vic.recompo.data.ai.ClaudeModels
import com.vic.recompo.data.db.dao.MensajeIADao
import com.vic.recompo.data.db.dao.SesionDao
import com.vic.recompo.data.db.dao.TipoSesionDao
import com.vic.recompo.data.db.dao.UsoIADao
import com.vic.recompo.data.db.entity.Conversacion
import com.vic.recompo.data.db.entity.MensajeIA
import com.vic.recompo.data.db.entity.UsoIA
import com.vic.recompo.domain.ai.ContextLimits
import com.vic.recompo.domain.ai.TarifasIA
import com.vic.recompo.domain.model.FuncionIA
import com.vic.recompo.domain.model.ProveedorIA
import com.vic.recompo.domain.model.RolMensaje
import com.vic.recompo.domain.model.TipoConversacion
import com.vic.recompo.domain.model.UserSettings
import com.vic.recompo.domain.usecase.CalcularKcalObjetivoUseCase
import com.vic.recompo.domain.usecase.ChatUseCase
import com.vic.recompo.ui.home.TipoDia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.Period

data class ChatUiState(
    val mensajes: List<MensajeIA> = emptyList(),
    val inputTexto: String = "",
    val enviando: Boolean = false,
    val error: String? = null,
    val cargando: Boolean = true
)

class ChatViewModel(
    private val conversacionDao: ConversacionDao,
    private val mensajeIADao: MensajeIADao,
    private val usoIADao: UsoIADao,
    private val settingsStore: UserSettingsStore,
    private val entradaComidaDao: EntradaComidaDao,
    private val sesionDao: SesionDao,
    private val actividadDao: ActividadDao,
    private val medicionDao: MedicionDao,
    private val tipoSesionDao: TipoSesionDao,
    private val chatUseCase: ChatUseCase,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var conversacionId: Long = -1L

    init {
        viewModelScope.launch {
            conversacionId = obtenerOCrearConversacion()
            mensajeIADao.getByConversacion(conversacionId).collect { mensajes ->
                _uiState.update { it.copy(mensajes = mensajes, cargando = false) }
            }
        }
    }

    private suspend fun obtenerOCrearConversacion(): Long {
        val todas = conversacionDao.getAll().first()
        if (todas.isNotEmpty()) return todas.first().id
        return conversacionDao.insert(
            Conversacion(
                tipo = TipoConversacion.NUTRICIONAL,
                fechaCreacion = Instant.now(),
                titulo = null
            )
        )
    }

    fun actualizarInput(texto: String) {
        _uiState.update { it.copy(inputTexto = texto) }
    }

    fun enviarMensaje() {
        val texto = _uiState.value.inputTexto.trim()
        if (texto.isBlank() || _uiState.value.enviando || conversacionId == -1L) return

        viewModelScope.launch {
            _uiState.update { it.copy(inputTexto = "", enviando = true, error = null) }

            mensajeIADao.insert(
                MensajeIA(
                    conversacionId = conversacionId,
                    rol = RolMensaje.USER,
                    contenido = texto,
                    tokensIn = null,
                    tokensOut = null,
                    timestamp = Instant.now()
                )
            )

            val settings = settingsStore.settings.first()
            if (settings == null) {
                _uiState.update { it.copy(enviando = false, error = "Perfil no configurado") }
                return@launch
            }

            val systemPrompt = construirSystemPrompt(settings)
            val historial = mensajeIADao.getByConversacion(conversacionId).first()
            val mensajesApi = truncarParaApi(
                systemPrompt,
                historial.map { Message(role = it.rol.name.lowercase(), content = it.contenido) }
            )

            when (val resultado = chatUseCase.enviar(systemPrompt, mensajesApi)) {
                is ChatUseCase.Resultado.Exito -> {
                    val ahora = Instant.now()
                    mensajeIADao.insert(
                        MensajeIA(
                            conversacionId = conversacionId,
                            rol = RolMensaje.ASSISTANT,
                            contenido = resultado.texto,
                            tokensIn = resultado.tokensIn,
                            tokensOut = resultado.tokensOut,
                            timestamp = ahora
                        )
                    )
                    usoIADao.insert(UsoIA(
                        timestamp = ahora,
                        funcion = FuncionIA.CHAT,
                        proveedor = ProveedorIA.CLAUDE_NATIVO,
                        modelo = ClaudeModels.DEFAULT_MODEL,
                        tokensIn = resultado.tokensIn,
                        tokensOut = resultado.tokensOut,
                        costeUsd = TarifasIA.calcularCosteUsd(ClaudeModels.DEFAULT_MODEL, resultado.tokensIn, resultado.tokensOut)
                    ))
                    _uiState.update { it.copy(enviando = false) }
                }
                is ChatUseCase.Resultado.Fallo ->
                    _uiState.update { it.copy(enviando = false, error = resultado.mensaje) }
            }
        }
    }

    fun descartarError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun construirSystemPrompt(settings: UserSettings): String {
        val template = context.assets.open("prompts/system_base.txt")
            .bufferedReader().use { it.readText() }
        val hoy = LocalDate.now()
        val edad = Period.between(settings.fechaNacimiento, hoy).years

        val actividades = actividadDao.getByFecha(hoy).first()
        val sesionActiva = sesionDao.getActivas().first().firstOrNull()
        val tipoDiaEnum = when {
            actividades.any { it.tipo.lowercase() == "bici" } -> TipoDia.BICI
            sesionActiva != null -> TipoDia.MUSCULACION
            else -> TipoDia.DESCANSO
        }
        val kcalObjetivo = CalcularKcalObjetivoUseCase().calcularObjetivoNutricional(
            tipoDia = tipoDiaEnum,
            kcalDescanso = settings.kcalDescanso,
            kcalMusculacion = settings.kcalMusculacion,
            kcalBici = settings.kcalBici
        )
        val tipoDiaTexto = when (tipoDiaEnum) {
            TipoDia.MUSCULACION -> "musculación"
            TipoDia.BICI -> "bici"
            TipoDia.DESCANSO -> "descanso"
        }

        val basePrompt = template
            .replace("{NOMBRE}", settings.nombre)
            .replace("{EDAD}", edad.toString())
            .replace("{ALTURA_CM}", settings.alturaCm.toString())
            .replace("{FASE_ACTUAL}", settings.faseActual)
            .replace("{FECHA_INICIO}", settings.fechaInicioPlan.toString())
            .replace("{PESO_INICIAL_KG}", settings.pesoInicialKg.toString())
            .replace("{PESO_OBJETIVO_KG}", settings.pesoObjetivoKg.toString())
            .replace("{TIPO_DIA}", tipoDiaTexto)
            .replace("{KCAL_OBJETIVO}", kcalObjetivo.toString())
            .replace("{PROTEINA_G}", settings.proteinaObjetivoG.toString())
            .replace("{KCAL_DESCANSO}", settings.kcalDescanso.toString())
            .replace("{KCAL_MUSCULACION}", settings.kcalMusculacion.toString())
            .replace("{KCAL_BICI}", settings.kcalBici.toString())
            .replace("{METABOLISMO_BASAL}", settings.metabolismoBasalKcal.toString())

        val totales = entradaComidaDao.getTotalesDelDia(hoy)
        val mediciones = medicionDao.getRecientes(ContextLimits.MAX_MEDICIONES_HISTORICO)

        val contextoHoy = buildString {
            append("\n\nContexto del día ($hoy):")
            append("\nTipo de día: $tipoDiaTexto — objetivo $kcalObjetivo kcal.")
            if (totales != null) {
                append("\nNutrición registrada: ${totales.kcal} kcal / ${totales.proteinaG.toInt()}g prot / ${totales.grasaG.toInt()}g grasa / ${totales.carboG.toInt()}g carbo.")
            } else {
                append("\nNutrición registrada: ninguna aún.")
            }
            if (sesionActiva != null) {
                val tipoNombre = tipoSesionDao.getById(sesionActiva.tipoSesionId)?.nombre ?: "?"
                append("\nSesión hoy: $tipoNombre (${sesionActiva.estado.name}).")
            } else {
                append("\nSin sesión activa hoy.")
            }
            if (mediciones.isNotEmpty()) {
                append("\n\nÚltimas ${mediciones.size} mediciones:")
                mediciones.forEach { m ->
                    append("\n  ${m.fecha}: ${m.pesoKg}kg")
                    m.grasaPct?.let { append(" / ${"%.1f".format(it)}% grasa") }
                    m.masaMagraKg?.let { append(" / ${"%.1f".format(it)}kg magra") }
                }
            }
        }

        return basePrompt + contextoHoy
    }

    private fun truncarParaApi(systemPrompt: String, mensajes: List<Message>): List<Message> {
        var resultado = if (mensajes.size > ContextLimits.MAX_MENSAJES_CHAT)
            mensajes.takeLast(ContextLimits.MAX_MENSAJES_CHAT) else mensajes
        while (resultado.isNotEmpty()) {
            val total = systemPrompt.length + resultado.sumOf { it.content.length }
            if (total <= ContextLimits.MAX_PROMPT_CHARS) break
            resultado = resultado.drop(1)
        }
        return resultado
    }
}

class ChatViewModelFactory(
    private val conversacionDao: ConversacionDao,
    private val mensajeIADao: MensajeIADao,
    private val usoIADao: UsoIADao,
    private val settingsStore: UserSettingsStore,
    private val entradaComidaDao: EntradaComidaDao,
    private val sesionDao: SesionDao,
    private val actividadDao: ActividadDao,
    private val medicionDao: MedicionDao,
    private val tipoSesionDao: TipoSesionDao,
    private val chatUseCase: ChatUseCase,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ChatViewModel(
            conversacionDao, mensajeIADao, usoIADao, settingsStore,
            entradaComidaDao, sesionDao, actividadDao, medicionDao, tipoSesionDao,
            chatUseCase, context
        ) as T
}
