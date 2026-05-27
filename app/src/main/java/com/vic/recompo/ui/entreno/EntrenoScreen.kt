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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import com.vic.recompo.data.db.entity.Sesion
import com.vic.recompo.domain.model.EstadoSesion
import com.vic.recompo.domain.model.TipoSesion
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EntrenoScreen(viewModel: EntrenoViewModel) {
    val state by viewModel.uiState.collectAsState()

    when (state.fase) {
        EntrenoFase.LISTA -> PantallaLista(
            state = state,
            onAbrirCrear = viewModel::abrirCrearSesion,
            onCerrarCrear = viewModel::cerrarCrearSesion,
            onTipoChanged = viewModel::onTipoNuevaSesionChanged,
            onFechaChanged = viewModel::onFechaNuevaSesionChanged,
            onMostrarDatePicker = viewModel::mostrarDatePicker,
            onCrear = viewModel::crearSesion,
            onAbrirSesion = viewModel::abrirPreSesion,
            onPedirBorrado = viewModel::pedirConfirmarBorrado,
            onCerrarBorrado = viewModel::cerrarConfirmarBorrado,
            onConfirmarBorrado = viewModel::confirmarEliminarSesion
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
    onAbrirCrear: () -> Unit,
    onCerrarCrear: () -> Unit,
    onTipoChanged: (TipoSesion) -> Unit,
    onFechaChanged: (LocalDate) -> Unit,
    onMostrarDatePicker: (Boolean) -> Unit,
    onCrear: () -> Unit,
    onAbrirSesion: (Sesion) -> Unit,
    onPedirBorrado: (Sesion) -> Unit,
    onCerrarBorrado: () -> Unit,
    onConfirmarBorrado: () -> Unit
) {
    val hoy = LocalDate.now()
    val sesionesDeHoy = state.sesiones.filter { it.fechaPrevista == hoy }
    val sesionesAnteriores = state.sesiones.filter { it.fechaPrevista != hoy }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAbrirCrear) {
                Icon(Icons.Default.Add, contentDescription = "Nueva sesión")
            }
        }
    ) { padding ->
        if (state.sesiones.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Sin sesiones todavía", style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Pulsa + para crear la primera", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stickyHeader { CabeceraSeccion(formatearHoy(hoy)) }
                if (sesionesDeHoy.isEmpty()) {
                    item { CardSinSesionHoy() }
                } else {
                    items(sesionesDeHoy, key = { it.id }) { sesion ->
                        TarjetaSesionLista(
                            sesion = sesion,
                            esHoy = true,
                            onClick = { onAbrirSesion(sesion) },
                            onEliminar = { onPedirBorrado(sesion) }
                        )
                    }
                }

                if (sesionesAnteriores.isNotEmpty()) {
                    stickyHeader { CabeceraSeccion("ANTERIORES") }
                    items(sesionesAnteriores, key = { it.id }) { sesion ->
                        TarjetaSesionLista(
                            sesion = sesion,
                            esHoy = false,
                            onClick = { onAbrirSesion(sesion) },
                            onEliminar = { onPedirBorrado(sesion) }
                        )
                    }
                }
            }
        }
    }

    if (state.dialogCrearSesion) {
        DialogCrearSesion(
            tipo = state.tipoNuevaSesion,
            fecha = state.fechaNuevaSesion,
            error = state.errorCrearSesion,
            onTipoChanged = onTipoChanged,
            onAbrirDatePicker = { onMostrarDatePicker(true) },
            onConfirmar = onCrear,
            onDismiss = onCerrarCrear
        )
    }

    if (state.showDatePicker) {
        DialogDatePicker(
            fechaInicial = state.fechaNuevaSesion,
            onFechaSeleccionada = {
                onFechaChanged(it)
                onMostrarDatePicker(false)
            },
            onDismiss = { onMostrarDatePicker(false) }
        )
    }

    state.dialogConfirmarBorrado?.let { sesion ->
        DialogConfirmarBorrado(
            sesion = sesion,
            onConfirmar = onConfirmarBorrado,
            onDismiss = onCerrarBorrado
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
private fun CardSinSesionHoy() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sin sesión para hoy", style = MaterialTheme.typography.titleSmall)
            Text(
                "Pulsa + para crear una",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaSesionLista(
    sesion: Sesion,
    esHoy: Boolean,
    onClick: () -> Unit,
    onEliminar: () -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("EEE d MMM", Locale("es"))
    val (estadoLabel, defaultColor) = when (sesion.estado) {
        EstadoSesion.EN_CURSO -> "En curso" to MaterialTheme.colorScheme.primaryContainer
        EstadoSesion.PLANIFICADA -> "Planificada" to MaterialTheme.colorScheme.secondaryContainer
        EstadoSesion.COMPLETADA -> "Completada" to MaterialTheme.colorScheme.surfaceVariant
        EstadoSesion.OMITIDA -> "Omitida" to MaterialTheme.colorScheme.errorContainer
    }
    val containerColor = if (esHoy) MaterialTheme.colorScheme.primaryContainer else defaultColor

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Sesión ${sesion.tipo}", style = MaterialTheme.typography.titleMedium)
                Text(
                    sesion.fechaPrevista.format(fmt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    estadoLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogCrearSesion(
    tipo: TipoSesion,
    fecha: LocalDate,
    error: String?,
    onTipoChanged: (TipoSesion) -> Unit,
    onAbrirDatePicker: () -> Unit,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val fmt = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale("es"))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva sesión") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = "Sesión ${tipo.name}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        TipoSesion.entries.forEach { t ->
                            DropdownMenuItem(
                                text = { Text("Sesión ${t.name}") },
                                onClick = { onTipoChanged(t); expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = fecha.format(fmt),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha") },
                    trailingIcon = {
                        IconButton(onClick = onAbrirDatePicker) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Elegir fecha")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirmar) { Text("Crear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogDatePicker(
    fechaInicial: LocalDate,
    onFechaSeleccionada: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val initialMillis = fechaInicial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis ?: initialMillis
                val fecha = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                onFechaSeleccionada(fecha)
            }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) {
        DatePicker(state = state)
    }
}

@Composable
private fun DialogConfirmarBorrado(
    sesion: Sesion,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es"))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Borrar sesión") },
        text = {
            Text("¿Borrar la sesión ${sesion.tipo} del ${sesion.fechaPrevista.format(fmt)}? Se eliminarán también todas sus series.")
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

private fun formatearHoy(hoy: LocalDate): String {
    val fmt = DateTimeFormatter.ofPattern("EEEE d MMM", Locale("es"))
    return "HOY · ${hoy.format(fmt)}"
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
    val sesion = state.sesionActual ?: return
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sesión ${sesion.tipo} — Pre-sesión") },
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
                ec.ejercicio.notasTecnica?.let {
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
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
    onIrAPostSesion: () -> Unit
) {
    val sesion = state.sesionActual ?: return
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sesión ${sesion.tipo} — En curso") })
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
                        onRegistrarSerie = { onRegistrarSerie(ec.ejercicioEnSesion.id) }
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
}

@Composable
private fun TarjetaEjercicioEnCurso(ec: EjercicioConSeries, onRegistrarSerie: () -> Unit) {
    val ees = ec.ejercicioEnSesion
    val seriesCompletadas = ec.series.size
    val seriesTotal = ees.seriesObjetivo
    val cargaObjetivo = ees.cargaObjetivoKg?.let { "${it}kg" } ?: "—"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(ec.ejercicio.nombre, style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f))
                Text(
                    "${seriesCompletadas}/${seriesTotal}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (seriesCompletadas >= seriesTotal)
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
                    Text(
                        "  S${serie.numero}: ${serie.repsReales} reps · ${serie.cargaKg}kg",
                        style = MaterialTheme.typography.bodySmall
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
            if (seriesCompletadas < seriesTotal) {
                FilledTonalButton(
                    onClick = onRegistrarSerie,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Registrar serie ${seriesCompletadas + 1}")
                }
            }
        }
    }
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
    val sesion = state.sesionActual ?: return
    val totalSeries = state.ejerciciosConSeries.sumOf { it.series.size }
    val totalEjercicios = state.ejerciciosConSeries.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sesión ${sesion.tipo} — Resumen") },
                navigationIcon = {
                    IconButton(onClick = onVolverEnCurso) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a en curso")
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
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Resumen", style = MaterialTheme.typography.titleMedium)
                        Text("$totalEjercicios ejercicios · $totalSeries series totales",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            items(state.ejerciciosConSeries) { ec ->
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    val rirEj = ec.ejercicioEnSesion.rir?.let { " · RIR $it" } ?: ""
                    Text("${ec.ejercicio.nombre}$rirEj", style = MaterialTheme.typography.titleSmall)
                    ec.series.forEach { serie ->
                        Text(
                            "  S${serie.numero}: ${serie.repsReales} reps · ${serie.cargaKg}kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
            item {
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("Generar próxima sesión (Fase 2)")
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
