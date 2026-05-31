package com.vic.recompo.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vic.recompo.domain.model.Sexo
import com.vic.recompo.domain.model.UserSettings
import com.vic.recompo.ui.common.DialogDatePicker
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FECHA_FMT = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es"))

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.uiState.collectAsState()
    val settings = state.settings

    if (settings == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Cargando ajustes…", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(horizontal = 16.dp, vertical = 16.dp)),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CardPerfil(settings, viewModel)
        CardPlan(settings, viewModel)
        CardMacros(settings, viewModel)
        CardBackup(settings, viewModel, state.exportando)
        CardExport(settings, viewModel, state.exportando)
        CardIA(state.usoIa)
        Spacer(Modifier.height(16.dp))
    }

    state.mensajeResultado?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::descartarMensaje,
            title = { Text("Resultado") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = viewModel::descartarMensaje) { Text("OK") } }
        )
    }

    state.dialog?.let { dialog ->
        DialogHost(
            dialog = dialog,
            onValor = viewModel::onValorChanged,
            onFecha = viewModel::onFechaChanged,
            onSexo = viewModel::onSexoChanged,
            onConfirmar = viewModel::guardar,
            onCancelar = viewModel::cerrarDialog
        )
    }
}

@Composable
private fun CardSeccion(titulo: String, content: @Composable () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun CampoItem(
    titulo: String,
    valor: String,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    supporting: String? = null
) {
    val mod = if (onClick != null && enabled) Modifier.clickable { onClick() } else Modifier
    ListItem(
        modifier = mod.fillMaxWidth(),
        headlineContent = {
            Text(
                text = titulo,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        supportingContent = supporting?.let { { Text(it, style = MaterialTheme.typography.bodySmall) } },
        trailingContent = {
            Text(
                text = valor,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun CardPerfil(s: UserSettings, vm: SettingsViewModel) {
    CardSeccion("Perfil") {
        CampoItem(
            titulo = "Nombre",
            valor = s.nombre,
            onClick = { vm.abrirDialog(SettingsDialog.Texto(CampoTexto.NOMBRE, s.nombre)) }
        )
        HorizontalDivider()
        CampoItem(
            titulo = "Fecha de nacimiento",
            valor = s.fechaNacimiento.format(FECHA_FMT),
            onClick = { vm.abrirDialog(SettingsDialog.Fecha(CampoFecha.FECHA_NACIMIENTO, s.fechaNacimiento)) }
        )
        HorizontalDivider()
        CampoItem(
            titulo = "Sexo biológico",
            valor = if (s.sexo == Sexo.HOMBRE) "Hombre" else "Mujer",
            onClick = { vm.abrirDialog(SettingsDialog.EditarSexo(s.sexo)) }
        )
        HorizontalDivider()
        CampoItem(
            titulo = "Altura",
            valor = "${s.alturaCm} cm",
            onClick = { vm.abrirDialog(SettingsDialog.Entero(CampoEntero.ALTURA, s.alturaCm.toString())) }
        )
    }
}

@Composable
private fun CardPlan(s: UserSettings, vm: SettingsViewModel) {
    CardSeccion("Plan") {
        CampoItem(
            titulo = "Fase actual",
            valor = s.faseActual,
            onClick = { vm.abrirDialog(SettingsDialog.Texto(CampoTexto.FASE, s.faseActual)) }
        )
        HorizontalDivider()
        CampoItem(
            titulo = "Inicio del plan",
            valor = s.fechaInicioPlan.format(FECHA_FMT),
            onClick = { vm.abrirDialog(SettingsDialog.Fecha(CampoFecha.FECHA_INICIO_PLAN, s.fechaInicioPlan)) }
        )
        HorizontalDivider()
        CampoItem(
            titulo = "Peso inicial",
            valor = "${s.pesoInicialKg} kg",
            onClick = { vm.abrirDialog(SettingsDialog.Decimal(CampoDecimal.PESO_INICIAL, s.pesoInicialKg.toString())) }
        )
        HorizontalDivider()
        CampoItem(
            titulo = "Peso objetivo",
            valor = "${s.pesoObjetivoKg} kg",
            onClick = { vm.abrirDialog(SettingsDialog.Decimal(CampoDecimal.PESO_OBJETIVO, s.pesoObjetivoKg.toString())) }
        )
    }
}

@Composable
private fun CardMacros(s: UserSettings, vm: SettingsViewModel) {
    CardSeccion("Macros objetivo") {
        CampoItem(
            titulo = "Kcal descanso",
            valor = "${s.kcalDescanso} kcal",
            onClick = { vm.abrirDialog(SettingsDialog.Entero(CampoEntero.KCAL_DESCANSO, s.kcalDescanso.toString())) }
        )
        HorizontalDivider()
        CampoItem(
            titulo = "Kcal musculación",
            valor = "${s.kcalMusculacion} kcal",
            onClick = { vm.abrirDialog(SettingsDialog.Entero(CampoEntero.KCAL_MUSCULACION, s.kcalMusculacion.toString())) }
        )
        HorizontalDivider()
        CampoItem(
            titulo = "Kcal bici",
            valor = "${s.kcalBici} kcal",
            onClick = { vm.abrirDialog(SettingsDialog.Entero(CampoEntero.KCAL_BICI, s.kcalBici.toString())) }
        )
        HorizontalDivider()
        CampoItem(
            titulo = "Metabolismo basal",
            valor = "${s.metabolismoBasalKcal} kcal",
            supporting = "Gasto en reposo. Base para calcular el déficit real.",
            onClick = { vm.abrirDialog(SettingsDialog.Entero(CampoEntero.METABOLISMO_BASAL, s.metabolismoBasalKcal.toString())) }
        )
        HorizontalDivider()
        CampoItem(
            titulo = "Proteína objetivo",
            valor = "${s.proteinaObjetivoG} g",
            onClick = { vm.abrirDialog(SettingsDialog.Entero(CampoEntero.PROTEINA, s.proteinaObjetivoG.toString())) }
        )
    }
}

@Composable
private fun CardBackup(s: UserSettings, vm: SettingsViewModel, exportando: Boolean) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { vm.cambiarCarpetaBackup(it) }
    }
    CardSeccion("Backup") {
        CampoItem(
            titulo = "Carpeta backup",
            valor = if (s.carpetaBackupUri == null) "Sin configurar" else "Configurada",
            supporting = s.carpetaBackupUri,
            onClick = { launcher.launch(null) }
        )
        if (s.carpetaBackupUri != null) {
            HorizontalDivider()
            CampoItem(
                titulo = "Quitar carpeta backup",
                valor = "",
                onClick = { vm.abrirDialog(SettingsDialog.ConfirmarQuitarBackup()) }
            )
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = vm::hacerBackup,
                    enabled = !exportando
                ) {
                    if (exportando) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Hacer backup ahora")
                }
            }
        }
    }
}

@Composable
private fun CardExport(s: UserSettings, vm: SettingsViewModel, exportando: Boolean) {
    val tieneCarpeta = s.carpetaBackupUri != null
    CardSeccion("Exportar") {
        CampoItem(
            titulo = "Exportar a Excel",
            valor = "",
            enabled = tieneCarpeta && !exportando,
            supporting = if (tieneCarpeta) "Genera recomposicion_YYYY-MM-DD.xlsx en la carpeta de backup"
                         else "Configura la carpeta de backup primero",
            onClick = { vm.exportarXlsx() }
        )
        HorizontalDivider()
        CampoItem(
            titulo = "Exportar todo (ZIP)",
            valor = "",
            enabled = tieneCarpeta && !exportando,
            supporting = if (tieneCarpeta) "JSON + Excel + SQLite en un ZIP"
                         else "Configura la carpeta de backup primero",
            onClick = { vm.exportarZip() }
        )
    }
}

@Composable
private fun CardIA(usoIa: UsoIaStats?) {
    CardSeccion("Uso de IA") {
        if (usoIa == null || usoIa.llamadasSemana == 0) {
            Text(
                text = "Sin llamadas a la IA en los últimos 7 días.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            CampoItem(
                titulo = "Total (7 días)",
                valor = "~€${"%.4f".format(usoIa.costeEurSemana)}",
                supporting = "${usoIa.llamadasSemana} llamadas · \$3/M in + \$15/M out · 1 USD ≈ 0,92 EUR"
            )
            usoIa.desglose.forEach { d ->
                HorizontalDivider()
                CampoItem(
                    titulo = etiquetaFuncion(d.funcion),
                    valor = "~€${"%.4f".format(d.costeEur)}",
                    supporting = "${d.llamadas} llamadas"
                )
            }
            if (usoIa.alertaTokensAltos) {
                HorizontalDivider()
                Text(
                    text = "⚠ Los tokens medios han subido más de un 20 % respecto a la semana anterior. Posible prompt hinchado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private fun etiquetaFuncion(funcion: com.vic.recompo.domain.model.FuncionIA): String = when (funcion) {
    com.vic.recompo.domain.model.FuncionIA.PARSEO_COMIDA -> "Parseo de comida"
    com.vic.recompo.domain.model.FuncionIA.GENERACION_SESION -> "Generación de sesión"
    com.vic.recompo.domain.model.FuncionIA.CHAT -> "Chat"
}

@Composable
private fun DialogHost(
    dialog: SettingsDialog,
    onValor: (String) -> Unit,
    onFecha: (java.time.LocalDate) -> Unit,
    onSexo: (Sexo) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    when (dialog) {
        is SettingsDialog.Texto -> DialogTextoEditor(
            titulo = tituloDe(dialog.campo),
            valor = dialog.valor,
            error = dialog.error,
            keyboard = KeyboardType.Text,
            onValor = onValor,
            onConfirmar = onConfirmar,
            onCancelar = onCancelar
        )
        is SettingsDialog.Entero -> DialogTextoEditor(
            titulo = tituloDe(dialog.campo),
            valor = dialog.valor,
            error = dialog.error,
            keyboard = KeyboardType.Number,
            onValor = onValor,
            onConfirmar = onConfirmar,
            onCancelar = onCancelar
        )
        is SettingsDialog.Decimal -> DialogTextoEditor(
            titulo = tituloDe(dialog.campo),
            valor = dialog.valor,
            error = dialog.error,
            keyboard = KeyboardType.Decimal,
            onValor = onValor,
            onConfirmar = onConfirmar,
            onCancelar = onCancelar
        )
        is SettingsDialog.Fecha -> DialogDatePicker(
            fechaInicial = dialog.valor,
            onFechaSeleccionada = { onFecha(it); onConfirmar() },
            onDismiss = onCancelar
        )
        is SettingsDialog.EditarSexo -> DialogSexo(
            seleccion = dialog.valor,
            onSexo = onSexo,
            onConfirmar = onConfirmar,
            onCancelar = onCancelar
        )
        is SettingsDialog.ConfirmarQuitarBackup -> AlertDialog(
            onDismissRequest = onCancelar,
            title = { Text("Quitar carpeta de backup") },
            text = { Text("Tendrás que volver a elegir una carpeta para que se guarden copias.") },
            confirmButton = { TextButton(onClick = onConfirmar) { Text("Quitar") } },
            dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DialogTextoEditor(
    titulo: String,
    valor: String,
    error: String?,
    keyboard: KeyboardType,
    onValor: (String) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = {
            Column {
                OutlinedTextField(
                    value = valor,
                    onValueChange = onValor,
                    singleLine = true,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirmar) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogSexo(
    seleccion: Sexo,
    onSexo: (Sexo) -> Unit,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Sexo biológico") },
        text = {
            Column {
                FilaSexo("Hombre", Sexo.HOMBRE, seleccion, onSexo)
                FilaSexo("Mujer", Sexo.MUJER, seleccion, onSexo)
            }
        },
        confirmButton = { TextButton(onClick = onConfirmar) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

@Composable
private fun FilaSexo(label: String, sexo: Sexo, seleccion: Sexo, onSexo: (Sexo) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSexo(sexo) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = seleccion == sexo, onClick = { onSexo(sexo) })
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

private fun tituloDe(campo: CampoTexto): String = when (campo) {
    CampoTexto.NOMBRE -> "Nombre"
    CampoTexto.FASE -> "Fase actual"
}

private fun tituloDe(campo: CampoEntero): String = when (campo) {
    CampoEntero.ALTURA -> "Altura (cm)"
    CampoEntero.KCAL_DESCANSO -> "Kcal descanso"
    CampoEntero.KCAL_MUSCULACION -> "Kcal musculación"
    CampoEntero.KCAL_BICI -> "Kcal bici"
    CampoEntero.METABOLISMO_BASAL -> "Metabolismo basal (kcal)"
    CampoEntero.PROTEINA -> "Proteína objetivo (g/día)"
}

private fun tituloDe(campo: CampoDecimal): String = when (campo) {
    CampoDecimal.PESO_INICIAL -> "Peso inicial (kg)"
    CampoDecimal.PESO_OBJETIVO -> "Peso objetivo (kg)"
}
