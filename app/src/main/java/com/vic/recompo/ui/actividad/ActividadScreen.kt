package com.vic.recompo.ui.actividad

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vic.recompo.data.db.entity.Actividad
import java.time.format.DateTimeFormatter

private val FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun ActividadScreen(viewModel: ActividadViewModel) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::abrirNueva) {
                Icon(Icons.Default.Add, contentDescription = "Nueva actividad")
            }
        }
    ) { padding ->
        if (state.actividades.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Sin actividades registradas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.height(8.dp)) }
                items(state.actividades, key = { it.id }) { actividad ->
                    ActividadCard(
                        actividad = actividad,
                        onClick = { viewModel.abrirEditar(actividad) },
                        onDelete = { viewModel.borrar(actividad.id) }
                    )
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }

        state.form?.let { form ->
            ActividadDialog(
                form = form,
                onTipoChanged = viewModel::onTipoChanged,
                onDescripcionChanged = viewModel::onDescripcionChanged,
                onDuracionMinChanged = viewModel::onDuracionMinChanged,
                onKcalQuemadasChanged = viewModel::onKcalQuemadasChanged,
                onDismiss = viewModel::cerrarDialog,
                onConfirm = viewModel::guardar
            )
        }
    }
}

@Composable
private fun ActividadCard(
    actividad: Actividad,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = actividad.tipo.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = actividad.fecha.format(FMT),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                actividad.descripcion?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val detalle = buildString {
                    append("${actividad.kcalQuemadas} kcal")
                    actividad.duracionMin?.let { append(" · ${it} min") }
                }
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Borrar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActividadDialog(
    form: ActividadFormState,
    onTipoChanged: (String) -> Unit,
    onDescripcionChanged: (String) -> Unit,
    onDuracionMinChanged: (String) -> Unit,
    onKcalQuemadasChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val titulo = if (form.id == null) "Nueva actividad" else "Editar actividad"
    val confirmEnabled = form.tipo.isNotBlank() && form.kcalQuemadas.toIntOrNull()?.let { it > 0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = form.tipo,
                    onValueChange = onTipoChanged,
                    label = { Text("Tipo") },
                    placeholder = { Text("bici, caminata, otro…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = form.kcalQuemadas,
                    onValueChange = onKcalQuemadasChanged,
                    label = { Text("Kcal quemadas *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = form.duracionMin,
                    onValueChange = onDuracionMinChanged,
                    label = { Text("Duración (min)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = form.descripcion,
                    onValueChange = onDescripcionChanged,
                    label = { Text("Descripción") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = confirmEnabled) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
