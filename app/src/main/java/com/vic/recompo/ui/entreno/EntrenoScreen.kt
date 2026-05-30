package com.vic.recompo.ui.entreno

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vic.recompo.data.db.entity.Ejercicio
import com.vic.recompo.data.db.entity.TipoSesion
import com.vic.recompo.domain.model.EstadoSerie
import com.vic.recompo.domain.model.EstadoSesion
import com.vic.recompo.domain.model.MotivoOmision
import com.vic.recompo.domain.usecase.ProximaSesionPropuesta
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EntrenoScreen(viewModel: EntrenoViewModel) {
    val state by viewModel.uiState.collectAsState()

    when (state.fase) {
        EntrenoFase.LISTA -> PantallaLista(
            state = state,
            onAbrirNuevaSesion = viewModel::abrirNuevaSesion,
            onCerrarNuevaSesion = viewModel::cerrarNuevaSesion,
            onTipoChanged = viewModel::onTipoNuevaSesionChanged,
            onCrearManual = viewModel::crearSesionManual,
            onGenerarIA = viewModel::iniciarGeneracionIA,
            onAbrirSesion = viewModel::abrirPreSesion,
            onPedirBorrado = viewModel::pedirConfirmarBorrado,
            onCerrarBorrado = viewModel::cerrarConfirmarBorrado,
            onConfirmarBorrado = viewModel::confirmarEliminarSesion,
            onAceptarPropuesta = viewModel::aceptarPropuesta,
            onRegenerarPropuesta = viewModel::regenerarPropuesta,
            onDescartarPropuesta = viewModel::descartarPropuesta,
            onUsarFallback = viewModel::usarFallback,
            onConfirmarReemplazar = viewModel::confirmarReemplazarPreparada,
            onCancelarReemplazar = viewModel::cancelarReemplazarPreparada,
            onConfirmarContextoSinHistorico = viewModel::confirmarContextoSinHistorico,
            onCancelarContextoSinHistorico = viewModel::cancelarContextoSinHistorico,
            onAbrirGestionTipos = viewModel::abrirGestionTipos,
            onCerrarGestionTipos = viewModel::cerrarGestionTipos,
            onAbrirCrearTipo = viewModel::abrirCrearTipo,
            onCerrarCrearTipo = viewModel::cerrarCrearTipo,
            onNombreNuevoTipoChanged = viewModel::onNombreNuevoTipoChanged,
            onConfirmarCrearTipo = viewModel::confirmarCrearTipo,
            onAbrirRenombrarTipo = viewModel::abrirRenombrarTipo,
            onCerrarRenombrarTipo = viewModel::cerrarRenombrarTipo,
            onNombreRenombrarChanged = viewModel::onNombreRenombrarChanged,
            onConfirmarRenombrarTipo = viewModel::confirmarRenombrarTipo,
            onToggleActivoTipo = viewModel::toggleActivoTipo
        )

        EntrenoFase.PRE_SESION -> PantallaPreSesion(
            state = state,
            onVolver = viewModel::volverALista,
            onAgregarEjercicio = viewModel::abrirAgregarEjercicio,
            onEliminarEjercicio = viewModel::eliminarEjercicio,
            onCerrarDialogEjercicio = viewModel::cerrarAgregarEjercicio,
            onFormEjercicioChanged = viewModel::onFormAgregarEjercicioChanged,
            onConfirmarEjercicio = viewModel::confirmarAgregarEjercicio,
            onIniciar = viewModel::iniciarSesion
        )

        EntrenoFase.EN_CURSO -> PantallaEnCurso(
            state = state,
            onRegistrarSerie = viewModel::abrirRegistrarSerie,
            onCerrarDialogSerie = viewModel::cerrarDialogSerie,
            onFormSerieChanged = viewModel::onFormSerieChanged,
            onGuardarSerie = viewModel::guardarSerie,
            onSaltarSerie = viewModel::abrirSaltarSerie,
            onCerrarSaltarSerie = viewModel::cerrarSaltarSerie,
            onMotivoChanged = viewModel::onMotivoOmisionChanged,
            onConfirmarSaltarSerie = viewModel::confirmarSaltarSerie,
            onIrAPostSesion = viewModel::irAPostSesion
        )

        EntrenoFase.POST_SESION -> PantallaPostSesion(
            state = state,
            onVolverEnCurso = viewModel::volverAEnCurso,
            onNotasChanged = viewModel::onNotasGlobalesChanged,
            onRirChanged = viewModel::onRirGlobalChanged,
            onCerrar = viewModel::cerrarSesion
        )
    }
}

// ── LISTA ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun PantallaLista(
    state: EntrenoUiState,
    onAbrirNuevaSesion: () -> Unit,
    onCerrarNuevaSesion: () -> Unit,
    onTipoChanged: (Long) -> Unit,
    onCrearManual: () -> Unit,
    onGenerarIA: () -> Unit,
    onAbrirSesion: (SesionConTipo) -> Unit,
    onPedirBorrado: (SesionConTipo) -> Unit,
    onCerrarBorrado: () -> Unit,
    onConfirmarBorrado: () -> Unit,
    onAceptarPropuesta: () -> Unit,
    onRegenerarPropuesta: () -> Unit,
    onDescartarPropuesta: () -> Unit,
    onUsarFallback: () -> Unit,
    onConfirmarReemplazar: () -> Unit,
    onCancelarReemplazar: () -> Unit,
    onConfirmarContextoSinHistorico: (String, Int?) -> Unit,
    onCancelarContextoSinHistorico: () -> Unit,
    onAbrirGestionTipos: () -> Unit,
    onCerrarGestionTipos: () -> Unit,
    onAbrirCrearTipo: () -> Unit,
    onCerrarCrearTipo: () -> Unit,
    onNombreNuevoTipoChanged: (String) -> Unit,
    onConfirmarCrearTipo: () -> Unit,
    onAbrirRenombrarTipo: (TipoSesion) -> Unit,
    onCerrarRenombrarTipo: () -> Unit,
    onNombreRenombrarChanged: (String) -> Unit,
    onConfirmarRenombrarTipo: () -> Unit,
    onToggleActivoTipo: (TipoSesion) -> Unit
) {
    val activas = state.sesiones.filter {
        it.sesion.estado == EstadoSesion.EN_CURSO || it.sesion.estado == EstadoSesion.PREPARADA
    }
    val completadas = state.sesiones.filter {
        it.sesion.estado == EstadoSesion.COMPLETADA || it.sesion.estado == EstadoSesion.OMITIDA
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Entrenos") },
                actions = {
                    IconButton(onClick = onAbrirGestionTipos) {
                        Icon(Icons.Default.Settings, contentDescription = "Gestionar tipos")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAbrirNuevaSesion) {
                Icon(Icons.Default.Add, contentDescription = "Nueva sesión")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.generandoSesion) {
                item {
                    CardGenerando()
                }
            }

            state.propuestaSesion?.let { propuesta ->
                item {
                    PropuestaCard(
                        propuesta = propuesta,
                        error = state.errorGeneracion,
                        onAceptar = onAceptarPropuesta,
                        onRegenerar = onRegenerarPropuesta,
                        onDescartar = onDescartarPropuesta
                    )
                }
            }

            if (state.propuestaSesion == null && !state.generandoSesion && state.errorGeneracion != null) {
                item {
                    ErrorGeneracionCard(
                        error = state.errorGeneracion,
                        onReintentar = onRegenerarPropuesta,
                        onDescartar = onDescartarPropuesta,
                        onUsarFallback = onUsarFallback
                    )
                }
            }

            if (activas.isNotEmpty() || completadas.isEmpty()) {
                stickyHeader { CabeceraSeccion("ACTIVAS") }
                if (activas.isEmpty()) {
                    item {
                        Text(
                            "Sin sesiones activas. Pulsa + para crear una.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                } else {
                    items(activas, key = { it.sesion.id }) { sct ->
                        TarjetaSesionLista(
                            sct = sct,
                            onClick = { onAbrirSesion(sct) },
                            onEliminar = { onPedirBorrado(sct) }
                        )
                    }
                }
            }

            if (completadas.isNotEmpty()) {
                stickyHeader { CabeceraSeccion("COMPLETADAS") }
                items(completadas, key = { it.sesion.id }) { sct ->
                    TarjetaSesionLista(
                        sct = sct,
                        onClick = { onAbrirSesion(sct) },
                        onEliminar = { onPedirBorrado(sct) }
                    )
                }
            }

            if (state.sesiones.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Sin sesiones todavía",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Pulsa + para crear la primera",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (state.dialogNuevaSesion) {
        DialogNuevaSesion(
            tiposSesion = state.tiposSesion,
            tipoSeleccionadoId = state.tipoNuevaSesionId,
            error = state.errorNuevaSesion,
            onTipoChanged = onTipoChanged,
            onCrearManual = onCrearManual,
            onGenerarIA = onGenerarIA,
            onDismiss = onCerrarNuevaSesion
        )
    }

    state.dialogConfirmarBorrado?.let { sct ->
        DialogConfirmarBorrado(
            sct = sct,
            onConfirmar = onConfirmarBorrado,
            onDismiss = onCerrarBorrado
        )
    }

    if (state.dialogConfirmarReemplazarPreparada != null) {
        DialogConfirmarReemplazarPreparada(
            onConfirmar = onConfirmarReemplazar,
            onDismiss = onCancelarReemplazar
        )
    }

    if (state.dialogContextoSinHistorico != null) {
        DialogContextoSinHistorico(
            onConfirmar = onConfirmarContextoSinHistorico,
            onDismiss = onCancelarContextoSinHistorico
        )
    }

    if (state.dialogGestionTipos) {
        DialogGestionTipos(
            tipos = state.tiposSesion,
            onToggleActivo = onToggleActivoTipo,
            onRenombrar = onAbrirRenombrarTipo,
            onCrearNuevo = onAbrirCrearTipo,
            onDismiss = onCerrarGestionTipos
        )
    }

    if (state.dialogCrearTipo) {
        DialogCrearTipo(
            nombre = state.nombreNuevoTipo,
            onNombreChanged = onNombreNuevoTipoChanged,
            onConfirmar = onConfirmarCrearTipo,
            onDismiss = onCerrarCrearTipo
        )
    }

    state.dialogRenombrarTipo?.let {
        DialogRenombrarTipo(
            nombre = state.nombreRenombrar,
            onNombreChanged = onNombreRenombrarChanged,
            onConfirmar = onConfirmarRenombrarTipo,
            onDismiss = onCerrarRenombrarTipo
        )
    }
}

@Composable
private fun CabeceraSeccion(texto: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            texto.uppercase(Locale("es")),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TarjetaSesionLista(
    sct: SesionConTipo,
    onClick: () -> Unit,
    onEliminar: () -> Unit
) {
    val fmtFecha = DateTimeFormatter.ofPattern("d MMM", Locale("es"))
    val fechaStr = sct.sesion.fechaEjecutada
        ?.atZone(ZoneId.systemDefault())?.toLocalDate()?.format(fmtFecha)

    val (estadoLabel, containerColor) = when (sct.sesion.estado) {
        EstadoSesion.EN_CURSO -> "En curso" to MaterialTheme.colorScheme.primaryContainer
        EstadoSesion.PREPARADA -> "Preparada" to MaterialTheme.colorScheme.secondaryContainer
        EstadoSesion.COMPLETADA -> "Completada" to MaterialTheme.colorScheme.surfaceVariant
        EstadoSesion.OMITIDA -> "Omitida" to MaterialTheme.colorScheme.errorContainer
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Sesión ${sct.tipoNombre}", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (fechaStr != null) "$estadoLabel · $fechaStr" else estadoLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                sct.sesion.notasIA?.let {
                    Text(
                        "IA: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1
                    )
                }
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Borrar sesión",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CardGenerando() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.height(24.dp).width(24.dp), strokeWidth = 3.dp)
            Text("Generando sesión con IA...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PropuestaCard(
    propuesta: ProximaSesionPropuesta,
    error: String?,
    onAceptar: () -> Unit,
    onRegenerar: () -> Unit,
    onDescartar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (error != null) {
                Text(
                    "Sin conexión IA — propuesta de respaldo",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text("Propuesta IA", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary)
            }
            if (propuesta.razonamiento.isNotBlank()) {
                Text(propuesta.razonamiento, style = MaterialTheme.typography.bodySmall)
            }
            propuesta.ejercicios.forEach { ep ->
                val cargaStr = ep.cargaObjetivoKg?.let { " · ${it}kg" } ?: ""
                Text(
                    "• ${ep.nombreEjercicio}  ${ep.seriesObjetivo}×${ep.repsMin}-${ep.repsMax}$cargaStr",
                    style = MaterialTheme.typography.bodySmall
                )
                ep.notas?.let { nota ->
                    Text("  $nota", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAceptar, modifier = Modifier.weight(1f)) { Text("Aceptar") }
                if (error == null) {
                    OutlinedButton(onClick = onRegenerar, modifier = Modifier.weight(1f)) { Text("Regenerar") }
                }
                TextButton(onClick = onDescartar) { Text("Descartar") }
            }
        }
    }
}

@Composable
private fun ErrorGeneracionCard(
    error: String,
    onReintentar: () -> Unit,
    onDescartar: () -> Unit,
    onUsarFallback: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Error al generar", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer)
            Text(error, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onReintentar) { Text("Reintentar") }
                OutlinedButton(onClick = onUsarFallback) { Text("Usar respaldo") }
                TextButton(onClick = onDescartar) { Text("Cancelar") }
            }
        }
    }
}

// ── DIALOGS LISTA ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogNuevaSesion(
    tiposSesion: List<TipoSesion>,
    tipoSeleccionadoId: Long?,
    error: String?,
    onTipoChanged: (Long) -> Unit,
    onCrearManual: () -> Unit,
    onGenerarIA: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val tipoSeleccionado = tiposSesion.find { it.id == tipoSeleccionadoId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva sesión") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (tiposSesion.isEmpty()) {
                    Text("No hay tipos de sesión disponibles.",
                        style = MaterialTheme.typography.bodyMedium)
                } else {
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = tipoSeleccionado?.let { "Sesión ${it.nombre}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            tiposSesion.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text("Sesión ${t.nombre}") },
                                    onClick = { onTipoChanged(t.id); expanded = false }
                                )
                            }
                        }
                    }
                }
                error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onCrearManual,
                    enabled = tipoSeleccionadoId != null
                ) { Text("Manual") }
                Button(
                    onClick = onGenerarIA,
                    enabled = tipoSeleccionadoId != null
                ) { Text("Con IA") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogConfirmarBorrado(
    sct: SesionConTipo,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Borrar sesión") },
        text = {
            Text("¿Borrar la sesión ${sct.tipoNombre}? Se eliminarán también todos sus ejercicios y series.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Borrar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogConfirmarReemplazarPreparada(
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sesión ya preparada") },
        text = {
            Text("Ya existe una sesión preparada de este tipo. Se eliminará y se generará una nueva con IA. ¿Continuar?")
        },
        confirmButton = {
            Button(onClick = onConfirmar) { Text("Reemplazar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogContextoSinHistorico(
    onConfirmar: (String, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var equipExpanded by remember { mutableStateOf(false) }
    var equipamiento by remember { mutableStateOf("GYM") }
    var tiempoStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Primera sesión") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Sin historial para este tipo. Indica el equipamiento disponible.",
                    style = MaterialTheme.typography.bodySmall
                )
                ExposedDropdownMenuBox(
                    expanded = equipExpanded,
                    onExpandedChange = { equipExpanded = it }
                ) {
                    OutlinedTextField(
                        value = equipamiento,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Equipamiento") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(equipExpanded) },
                        modifier = Modifier.fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = equipExpanded,
                        onDismissRequest = { equipExpanded = false }
                    ) {
                        listOf("GYM", "CASA").forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = { equipamiento = opt; equipExpanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = tiempoStr,
                    onValueChange = { tiempoStr = it },
                    label = { Text("Tiempo disponible (min, opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirmar(equipamiento, tiempoStr.toIntOrNull())
            }) { Text("Generar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogGestionTipos(
    tipos: List<TipoSesion>,
    onToggleActivo: (TipoSesion) -> Unit,
    onRenombrar: (TipoSesion) -> Unit,
    onCrearNuevo: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tipos de sesión") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                tipos.forEach { tipo ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Sesión ${tipo.nombre}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onRenombrar(tipo) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Renombrar",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = tipo.activo,
                            onCheckedChange = { onToggleActivo(tipo) }
                        )
                    }
                    HorizontalDivider()
                }
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = onCrearNuevo,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Nuevo tipo")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
private fun DialogCrearTipo(
    nombre: String,
    onNombreChanged: (String) -> Unit,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo tipo de sesión") },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = onNombreChanged,
                label = { Text("Nombre (ej. A, Cardio, Hombro)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                enabled = nombre.isNotBlank()
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogRenombrarTipo(
    nombre: String,
    onNombreChanged: (String) -> Unit,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Renombrar tipo") },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = onNombreChanged,
                label = { Text("Nuevo nombre") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                enabled = nombre.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── PRE-SESIÓN ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantallaPreSesion(
    state: EntrenoUiState,
    onVolver: () -> Unit,
    onAgregarEjercicio: () -> Unit,
    onEliminarEjercicio: (Long) -> Unit,
    onCerrarDialogEjercicio: () -> Unit,
    onFormEjercicioChanged: (FormAgregarEjercicio) -> Unit,
    onConfirmarEjercicio: () -> Unit,
    onIniciar: () -> Unit
) {
    val sesionConTipo = state.sesionActual ?: return
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sesión ${sesionConTipo.tipoNombre} — Pre-sesión") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.ejerciciosConSeries.isEmpty()) {
                    item {
                        Text(
                            "Sin ejercicios. Añade uno con el botón +.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
                items(state.ejerciciosConSeries) { ec ->
                    TarjetaEjercicioPreSesion(
                        ec = ec,
                        onEliminar = { onEliminarEjercicio(ec.ejercicioEnSesion.id) }
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAgregarEjercicio,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(" Añadir ejercicio")
                }
                Button(
                    onClick = onIniciar,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.ejerciciosConSeries.isNotEmpty()
                ) {
                    Text("Empezar sesión")
                }
            }
        }
    }

    state.dialogAgregarEjercicio?.let { form ->
        DialogAgregarEjercicio(
            form = form,
            ejerciciosDisponibles = state.ejerciciosDisponibles,
            onFormChanged = onFormEjercicioChanged,
            onConfirmar = onConfirmarEjercicio,
            onDismiss = onCerrarDialogEjercicio
        )
    }
}

@Composable
private fun TarjetaEjercicioPreSesion(ec: EjercicioConSeries, onEliminar: () -> Unit) {
    val ees = ec.ejercicioEnSesion
    val cargaTexto = ees.cargaObjetivoKg?.let { " · ${it}kg" } ?: ""
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(ec.ejercicio.nombre, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${ees.seriesObjetivo}×${ees.repsObjetivoMin}-${ees.repsObjetivoMax}$cargaTexto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ees.notas?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogAgregarEjercicio(
    form: FormAgregarEjercicio,
    ejerciciosDisponibles: List<Ejercicio>,
    onFormChanged: (FormAgregarEjercicio) -> Unit,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = ejerciciosDisponibles.find { it.id == form.ejercicioId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir ejercicio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selected?.nombre ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ejercicio") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        ejerciciosDisponibles.forEach { ej ->
                            DropdownMenuItem(
                                text = { Text(ej.nombre) },
                                onClick = { onFormChanged(form.copy(ejercicioId = ej.id)); expanded = false }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = form.seriesObjetivo,
                        onValueChange = { onFormChanged(form.copy(seriesObjetivo = it)) },
                        label = { Text("Series") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = form.repsMin,
                        onValueChange = { onFormChanged(form.copy(repsMin = it)) },
                        label = { Text("Reps mín") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = form.repsMax,
                        onValueChange = { onFormChanged(form.copy(repsMax = it)) },
                        label = { Text("Reps máx") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = form.cargaKg,
                    onValueChange = { onFormChanged(form.copy(cargaKg = it)) },
                    label = { Text("Carga objetivo (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                enabled = form.ejercicioId != null &&
                    form.seriesObjetivo.toIntOrNull() != null &&
                    form.repsMin.toIntOrNull() != null &&
                    form.repsMax.toIntOrNull() != null
            ) { Text("Añadir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── EN CURSO ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantallaEnCurso(
    state: EntrenoUiState,
    onRegistrarSerie: (Long) -> Unit,
    onCerrarDialogSerie: () -> Unit,
    onFormSerieChanged: (FormSerie) -> Unit,
    onGuardarSerie: () -> Unit,
    onSaltarSerie: (Long) -> Unit,
    onCerrarSaltarSerie: () -> Unit,
    onMotivoChanged: (MotivoOmision?) -> Unit,
    onConfirmarSaltarSerie: () -> Unit,
    onIrAPostSesion: () -> Unit
) {
    val sesionConTipo = state.sesionActual ?: return
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sesión ${sesionConTipo.tipoNombre} — En curso") })
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.ejerciciosConSeries) { ec ->
                    TarjetaEjercicioEnCurso(
                        ec = ec,
                        onRegistrarSerie = { onRegistrarSerie(ec.ejercicioEnSesion.id) },
                        onSaltarSerie = { onSaltarSerie(ec.ejercicioEnSesion.id) }
                    )
                }
            }
            Button(
                onClick = onIrAPostSesion,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("Cerrar sesión")
            }
        }
    }

    state.dialogSerie?.let { form ->
        DialogRegistrarSerie(
            form = form,
            onFormChanged = onFormSerieChanged,
            onConfirmar = onGuardarSerie,
            onDismiss = onCerrarDialogSerie
        )
    }

    state.dialogSaltarSerie?.let {
        DialogSaltarSerie(
            motivoSeleccionado = state.motivoOmisionSeleccionado,
            onMotivoChanged = onMotivoChanged,
            onConfirmar = onConfirmarSaltarSerie,
            onDismiss = onCerrarSaltarSerie
        )
    }
}

@Composable
private fun TarjetaEjercicioEnCurso(
    ec: EjercicioConSeries,
    onRegistrarSerie: () -> Unit,
    onSaltarSerie: () -> Unit
) {
    val ees = ec.ejercicioEnSesion
    val seriesRegistradas = ec.series.size
    val seriesTotal = ees.seriesObjetivo
    val cargaObjetivo = ees.cargaObjetivoKg?.let { "${it}kg" } ?: "—"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    ec.ejercicio.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$seriesRegistradas/$seriesTotal",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (seriesRegistradas >= seriesTotal)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "Objetivo: ${ees.repsObjetivoMin}-${ees.repsObjetivoMax} reps · $cargaObjetivo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (ec.series.isNotEmpty()) {
                ec.series.forEach { serie ->
                    val serieTexto = when (serie.estado) {
                        EstadoSerie.COMPLETADA ->
                            "S${serie.numero}: ${serie.repsReales} reps · ${serie.cargaKg}kg"
                        EstadoSerie.OMITIDA -> {
                            val motivo = serie.motivoOmision?.name?.let { " [$it]" } ?: ""
                            "S${serie.numero}: OMITIDA$motivo"
                        }
                    }
                    Text(
                        "  $serieTexto",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (serie.estado == EstadoSerie.OMITIDA)
                            MaterialTheme.colorScheme.onSurfaceVariant
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            ees.rir?.let {
                Text(
                    "RIR ejercicio: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (seriesRegistradas < seriesTotal) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onRegistrarSerie,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Serie ${seriesRegistradas + 1}")
                    }
                    OutlinedButton(onClick = onSaltarSerie) {
                        Text("Saltar")
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogSaltarSerie(
    motivoSeleccionado: MotivoOmision?,
    onMotivoChanged: (MotivoOmision?) -> Unit,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saltar serie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Motivo (opcional):", style = MaterialTheme.typography.bodySmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MotivoOmision.entries.forEach { motivo ->
                        FilterChip(
                            selected = motivoSeleccionado == motivo,
                            onClick = {
                                onMotivoChanged(if (motivoSeleccionado == motivo) null else motivo)
                            },
                            label = {
                                Text(
                                    when (motivo) {
                                        MotivoOmision.TIEMPO -> "Tiempo"
                                        MotivoOmision.INNECESARIA -> "Innec."
                                        MotivoOmision.MOLESTIA -> "Molest."
                                    }
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirmar) { Text("Saltar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun DialogRegistrarSerie(
    form: FormSerie,
    onFormChanged: (FormSerie) -> Unit,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    val titulo = if (form.esUltimaSerie) "Serie ${form.numero} (última) · RIR del ejercicio"
        else "Serie ${form.numero}"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = form.reps,
                        onValueChange = { onFormChanged(form.copy(reps = it)) },
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = form.cargaKg,
                        onValueChange = { onFormChanged(form.copy(cargaKg = it)) },
                        label = { Text("Carga (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                if (form.esUltimaSerie) {
                    Text("RIR del ejercicio: ${form.rir}", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        (0..5).forEach { rir ->
                            val isSelected = form.rir == rir
                            if (isSelected) {
                                Button(
                                    onClick = { onFormChanged(form.copy(rir = rir)) },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                ) { Text("$rir") }
                            } else {
                                OutlinedButton(
                                    onClick = { onFormChanged(form.copy(rir = rir)) },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                ) { Text("$rir") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                enabled = form.reps.toIntOrNull() != null && form.cargaKg.toDoubleOrNull() != null
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── POST-SESIÓN ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PantallaPostSesion(
    state: EntrenoUiState,
    onVolverEnCurso: () -> Unit,
    onNotasChanged: (String) -> Unit,
    onRirChanged: (String) -> Unit,
    onCerrar: () -> Unit
) {
    val sesionConTipo = state.sesionActual ?: return
    val totalEjercicios = state.ejerciciosConSeries.size
    val totalSeries = state.ejerciciosConSeries.sumOf { it.series.size }
    val seriesOmitidas = state.ejerciciosConSeries
        .sumOf { it.series.count { s -> s.estado == EstadoSerie.OMITIDA } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sesión ${sesionConTipo.tipoNombre} — Resumen") },
                navigationIcon = {
                    IconButton(onClick = onVolverEnCurso) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Resumen", style = MaterialTheme.typography.titleMedium)
                        val omitStr = if (seriesOmitidas > 0) " · $seriesOmitidas omitidas" else ""
                        Text(
                            "$totalEjercicios ejercicios · $totalSeries series$omitStr",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            items(state.ejerciciosConSeries) { ec ->
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    val rirEj = ec.ejercicioEnSesion.rir?.let { " · RIR $it" } ?: ""
                    Text(
                        "${ec.ejercicio.nombre}$rirEj",
                        style = MaterialTheme.typography.titleSmall
                    )
                    ec.series.forEach { serie ->
                        val serieTexto = when (serie.estado) {
                            EstadoSerie.COMPLETADA ->
                                "  S${serie.numero}: ${serie.repsReales} reps · ${serie.cargaKg}kg"
                            EstadoSerie.OMITIDA -> {
                                val motivo = serie.motivoOmision?.name?.let { " [$it]" } ?: ""
                                "  S${serie.numero}: OMITIDA$motivo"
                            }
                        }
                        Text(
                            serieTexto,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (serie.estado == EstadoSerie.OMITIDA)
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.notasGlobales,
                    onValueChange = onNotasChanged,
                    label = { Text("Notas de la sesión") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = state.rirGlobal,
                    onValueChange = onRirChanged,
                    label = { Text("RIR global (0-5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Button(onClick = onCerrar, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar sesión")
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
