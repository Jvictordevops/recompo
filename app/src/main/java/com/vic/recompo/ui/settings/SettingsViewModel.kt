package com.vic.recompo.ui.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vic.recompo.data.UserSettingsStore
import com.vic.recompo.data.backup.BackupSerializer
import com.vic.recompo.data.backup.XlsxExporter
import com.vic.recompo.data.backup.ZipExporter
import com.vic.recompo.data.db.RecompoDatabase
import com.vic.recompo.domain.model.Sexo
import com.vic.recompo.domain.model.UserSettings
import com.vic.recompo.domain.validation.UserSettingsValidation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class CampoTexto { NOMBRE, FASE }
enum class CampoEntero { ALTURA, KCAL_DESCANSO, KCAL_MUSCULACION, KCAL_BICI, METABOLISMO_BASAL, PROTEINA }
enum class CampoDecimal { PESO_INICIAL, PESO_OBJETIVO }
enum class CampoFecha { FECHA_NACIMIENTO, FECHA_INICIO_PLAN }

sealed class SettingsDialog {
    abstract val error: String?

    data class Texto(
        val campo: CampoTexto,
        val valor: String,
        override val error: String? = null
    ) : SettingsDialog()

    data class Entero(
        val campo: CampoEntero,
        val valor: String,
        override val error: String? = null
    ) : SettingsDialog()

    data class Decimal(
        val campo: CampoDecimal,
        val valor: String,
        override val error: String? = null
    ) : SettingsDialog()

    data class Fecha(
        val campo: CampoFecha,
        val valor: LocalDate,
        override val error: String? = null
    ) : SettingsDialog()

    data class EditarSexo(
        val valor: Sexo,
        override val error: String? = null
    ) : SettingsDialog()

    data class ConfirmarQuitarBackup(
        override val error: String? = null
    ) : SettingsDialog()
}

data class SettingsUiState(
    val settings: UserSettings? = null,
    val dialog: SettingsDialog? = null,
    val exportando: Boolean = false,
    val mensajeResultado: String? = null
)

class SettingsViewModel(
    application: Application,
    private val store: UserSettingsStore,
    private val database: RecompoDatabase
) : AndroidViewModel(application) {

    private val backupSerializer = BackupSerializer(application, database, store)
    private val xlsxExporter = XlsxExporter(database, store)
    private val zipExporter = ZipExporter(application, database, store, backupSerializer, xlsxExporter)

    private val _dialog = MutableStateFlow<SettingsDialog?>(null)
    private val _exportando = MutableStateFlow(false)
    private val _mensajeResultado = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        store.settings,
        _dialog,
        _exportando,
        _mensajeResultado
    ) { s, d, exp, msg -> SettingsUiState(settings = s, dialog = d, exportando = exp, mensajeResultado = msg) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun abrirDialog(dialog: SettingsDialog) { _dialog.value = dialog }

    fun cerrarDialog() { _dialog.value = null }

    fun onValorChanged(nuevo: String) {
        _dialog.update {
            when (it) {
                is SettingsDialog.Texto -> it.copy(valor = nuevo, error = null)
                is SettingsDialog.Entero -> it.copy(valor = nuevo, error = null)
                is SettingsDialog.Decimal -> it.copy(valor = nuevo, error = null)
                else -> it
            }
        }
    }

    fun onFechaChanged(nueva: LocalDate) {
        _dialog.update {
            if (it is SettingsDialog.Fecha) it.copy(valor = nueva, error = null) else it
        }
    }

    fun onSexoChanged(nuevo: Sexo) {
        _dialog.update {
            if (it is SettingsDialog.EditarSexo) it.copy(valor = nuevo, error = null) else it
        }
    }

    fun guardar() {
        val actuales = uiState.value.settings ?: return
        val d = _dialog.value ?: return
        val actualizado: UserSettings = when (d) {
            is SettingsDialog.Texto -> {
                val parsed = when (d.campo) {
                    CampoTexto.NOMBRE -> UserSettingsValidation.parseNombre(d.valor)
                    CampoTexto.FASE -> UserSettingsValidation.parseFase(d.valor)
                }
                val valor = parsed.getOrElse { setError(d, it.message); return }
                when (d.campo) {
                    CampoTexto.NOMBRE -> actuales.copy(nombre = valor)
                    CampoTexto.FASE -> actuales.copy(faseActual = valor)
                }
            }
            is SettingsDialog.Entero -> {
                val parsed = when (d.campo) {
                    CampoEntero.ALTURA -> UserSettingsValidation.parseAltura(d.valor)
                    CampoEntero.KCAL_DESCANSO,
                    CampoEntero.KCAL_MUSCULACION,
                    CampoEntero.KCAL_BICI,
                    CampoEntero.METABOLISMO_BASAL -> UserSettingsValidation.parseKcal(d.valor)
                    CampoEntero.PROTEINA -> UserSettingsValidation.parseProteinaG(d.valor)
                }
                val valor = parsed.getOrElse { setError(d, it.message); return }
                when (d.campo) {
                    CampoEntero.ALTURA -> actuales.copy(alturaCm = valor)
                    CampoEntero.KCAL_DESCANSO -> actuales.copy(kcalDescanso = valor)
                    CampoEntero.KCAL_MUSCULACION -> actuales.copy(kcalMusculacion = valor)
                    CampoEntero.KCAL_BICI -> actuales.copy(kcalBici = valor)
                    CampoEntero.METABOLISMO_BASAL -> actuales.copy(metabolismoBasalKcal = valor)
                    CampoEntero.PROTEINA -> actuales.copy(proteinaObjetivoG = valor)
                }
            }
            is SettingsDialog.Decimal -> {
                val errorMsg = when (d.campo) {
                    CampoDecimal.PESO_INICIAL -> UserSettingsValidation.ERROR_PESO_INICIAL
                    CampoDecimal.PESO_OBJETIVO -> UserSettingsValidation.ERROR_PESO_OBJETIVO
                }
                val valor = UserSettingsValidation.parsePesoKg(d.valor, errorMsg)
                    .getOrElse { setError(d, it.message); return }
                when (d.campo) {
                    CampoDecimal.PESO_INICIAL -> actuales.copy(pesoInicialKg = valor)
                    CampoDecimal.PESO_OBJETIVO -> actuales.copy(pesoObjetivoKg = valor)
                }
            }
            is SettingsDialog.Fecha -> when (d.campo) {
                CampoFecha.FECHA_NACIMIENTO -> actuales.copy(fechaNacimiento = d.valor)
                CampoFecha.FECHA_INICIO_PLAN -> actuales.copy(fechaInicioPlan = d.valor)
            }
            is SettingsDialog.EditarSexo -> actuales.copy(sexo = d.valor)
            is SettingsDialog.ConfirmarQuitarBackup -> {
                quitarCarpetaBackup()
                return
            }
        }
        persistir(actualizado)
    }

    fun cambiarCarpetaBackup(uri: Uri) {
        val ctx = getApplication<Application>()
        runCatching {
            ctx.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        viewModelScope.launch { store.saveBackupUri(uri.toString()) }
    }

    fun quitarCarpetaBackup() {
        viewModelScope.launch {
            store.clearBackupUri()
            _dialog.value = null
        }
    }

    fun descartarMensaje() { _mensajeResultado.value = null }

    fun hacerBackup() {
        if (_exportando.value) return
        viewModelScope.launch {
            _exportando.value = true
            backupSerializer.hacerBackup()
                .onSuccess { bytes -> _mensajeResultado.value = "Backup OK (${bytes / 1024} KB)" }
                .onFailure { e -> _mensajeResultado.value = "Error: ${e.message}" }
            _exportando.value = false
        }
    }

    fun exportarXlsx() {
        if (_exportando.value) return
        val uri = uiState.value.settings?.carpetaBackupUri
            ?: run { _mensajeResultado.value = "Configura la carpeta de backup primero"; return }
        viewModelScope.launch {
            _exportando.value = true
            runCatching {
                val bytes = xlsxExporter.generarBytes()
                val nombre = "recomposicion_${java.time.LocalDate.now()}.xlsx"
                backupSerializer.escribirEnCarpeta(uri, nombre, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes)
                bytes.size.toLong()
            }.onSuccess { bytes -> _mensajeResultado.value = "Excel exportado (${bytes / 1024} KB)" }
             .onFailure { e -> _mensajeResultado.value = "Error: ${e.message}" }
            _exportando.value = false
        }
    }

    fun exportarZip() {
        if (_exportando.value) return
        val uri = uiState.value.settings?.carpetaBackupUri
            ?: run { _mensajeResultado.value = "Configura la carpeta de backup primero"; return }
        viewModelScope.launch {
            _exportando.value = true
            runCatching {
                val bytes = zipExporter.generarBytes()
                val nombre = "recomposicion_backup_${java.time.LocalDate.now()}.zip"
                backupSerializer.escribirEnCarpeta(uri, nombre, "application/zip", bytes)
                bytes.size.toLong()
            }.onSuccess { bytes -> _mensajeResultado.value = "ZIP exportado (${bytes / 1024} KB)" }
             .onFailure { e -> _mensajeResultado.value = "Error: ${e.message}" }
            _exportando.value = false
        }
    }

    private fun persistir(s: UserSettings) {
        viewModelScope.launch {
            store.save(s)
            _dialog.value = null
        }
    }

    private fun setError(d: SettingsDialog, msg: String?) {
        _dialog.value = when (d) {
            is SettingsDialog.Texto -> d.copy(error = msg)
            is SettingsDialog.Entero -> d.copy(error = msg)
            is SettingsDialog.Decimal -> d.copy(error = msg)
            is SettingsDialog.Fecha -> d.copy(error = msg)
            is SettingsDialog.EditarSexo -> d.copy(error = msg)
            is SettingsDialog.ConfirmarQuitarBackup -> d.copy(error = msg)
        }
    }
}

class SettingsViewModelFactory(
    private val application: Application,
    private val store: UserSettingsStore,
    private val database: RecompoDatabase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        SettingsViewModel(application, store, database) as T
}
