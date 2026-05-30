package com.vic.recompo.ui.nutricion

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vic.recompo.data.db.entity.ComidaBase
import com.vic.recompo.data.db.entity.EntradaComida
import com.vic.recompo.domain.model.SlotComida

@Composable
fun NutricionScreen(viewModel: NutricionViewModel) {
    val state by viewModel.uiState.collectAsState()
    val dialog by viewModel.dialogState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TarjetaTotalesDelDia(state.totales) }
        SlotComida.entries.forEach { slot ->
            item {
                SlotSection(
                    slot = slot,
                    entradas = state.entradasPorSlot[slot] ?: emptyList(),
                    onAnadir = { viewModel.abrirDialogo(slot) },
                    onEditar = { entrada -> viewModel.abrirDialogo(slot, entrada) },
                    onBorrar = { id -> viewModel.borrar(id) }
                )
            }
        }
    }

    if (dialog.abierto) {
        AnadirComidaDialog(
            dialog = dialog,
            plantillas = state.plantillasPorSlot.values.flatten(),
            onModoPlantillaChanged = viewModel::onModoPlantillaChanged,
            onPlantillaSeleccionada = viewModel::onPlantillaSeleccionada,
            onTextoLibreChanged = viewModel::onTextoLibreChanged,
            onKcalChanged = viewModel::onKcalChanged,
            onProteinaChanged = viewModel::onProteinaChanged,
            onGrasaChanged = viewModel::onGrasaChanged,
            onCarboChanged = viewModel::onCarboChanged,
            onParsearConIA = viewModel::parsearConIA,
            onRespuestaAclaracionChanged = viewModel::onRespuestaAclaracionChanged,
            onEnviarAclaracion = viewModel::enviarAclaracion,
            onAceptarYGuardar = viewModel::aceptarYGuardar,
            onEditarAMano = viewModel::editarAMano,
            onAbrirAfinar = viewModel::abrirAfinar,
            onTextoAfinarChanged = viewModel::onTextoAfinarChanged,
            onEnviarAfinar = viewModel::enviarAfinar,
            onGuardar = viewModel::guardar,
            onCancelar = viewModel::cerrarDialogo
        )
    }
}

@Composable
private fun TarjetaTotalesDelDia(totales: TotalesDelDia) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Hoy", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${totales.kcal} kcal", style = MaterialTheme.typography.headlineSmall)
            Text(
                "${totales.proteinaG.toInt()}g prot · " +
                "${totales.grasaG.toInt()}g grasa · " +
                "${totales.carboG.toInt()}g carbo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SlotSection(
    slot: SlotComida,
    entradas: List<EntradaComida>,
    onAnadir: () -> Unit,
    onEditar: (EntradaComida) -> Unit,
    onBorrar: (Long) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(slot.etiqueta(), style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = onAnadir) {
                Icon(Icons.Default.Add, contentDescription = "Añadir ${slot.etiqueta()}")
            }
        }
        if (entradas.isEmpty()) {
            Text(
                "Sin entradas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )
        } else {
            entradas.forEach { entrada ->
                EntradaItem(
                    entrada = entrada,
                    onClick = { onEditar(entrada) },
                    onBorrar = { onBorrar(entrada.id) }
                )
                Spacer(Modifier.height(4.dp))
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun EntradaItem(
    entrada: EntradaComida,
    onClick: () -> Unit,
    onBorrar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entrada.textoLibre, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${entrada.proteinaG.toInt()}g prot · " +
                    "${entrada.grasaG.toInt()}g grasa · " +
                    "${entrada.carboG.toInt()}g carbo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("${entrada.kcal} kcal", style = MaterialTheme.typography.labelLarge)
            IconButton(onClick = onBorrar) {
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
private fun AnadirComidaDialog(
    dialog: DialogState,
    plantillas: List<ComidaBase>,
    onModoPlantillaChanged: (Boolean) -> Unit,
    onPlantillaSeleccionada: (ComidaBase) -> Unit,
    onTextoLibreChanged: (String) -> Unit,
    onKcalChanged: (String) -> Unit,
    onProteinaChanged: (String) -> Unit,
    onGrasaChanged: (String) -> Unit,
    onCarboChanged: (String) -> Unit,
    onParsearConIA: () -> Unit,
    onRespuestaAclaracionChanged: (String) -> Unit,
    onEnviarAclaracion: () -> Unit,
    onAceptarYGuardar: () -> Unit,
    onEditarAMano: () -> Unit,
    onAbrirAfinar: () -> Unit,
    onTextoAfinarChanged: (String) -> Unit,
    onEnviarAfinar: () -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    val titulo = if (dialog.entradaEditando != null) "Editar entrada"
    else "Añadir ${dialog.slot.etiqueta()}"

    val puedoGuardar = dialog.textoLibre.isNotBlank() &&
        dialog.kcal.toIntOrNull() != null &&
        dialog.proteinaG.toDoubleOrNull() != null &&
        dialog.grasaG.toDoubleOrNull() != null &&
        dialog.carboG.toDoubleOrNull() != null

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    dialog.resultadoIA != null -> {
                        ResultadoIACard(resultado = dialog.resultadoIA, onEditarAMano = onEditarAMano)
                        if (dialog.afinando) {
                            OutlinedTextField(
                                value = dialog.textoAfinar,
                                onValueChange = onTextoAfinarChanged,
                                label = { Text("Añade una precisión") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = onEnviarAfinar,
                                enabled = dialog.textoAfinar.isNotBlank() && !dialog.parseando,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (dialog.parseando) SpinnerConTexto("Recalculando...")
                                else Text("Recalcular")
                            }
                        } else {
                            TextButton(
                                onClick = onAbrirAfinar,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) { Text("Afinar con una precisión") }
                        }
                        dialog.errorIA?.let { err ->
                            OutlinedTextField(
                                value = err, onValueChange = {}, readOnly = true, isError = true,
                                label = { Text("Error IA", color = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.fillMaxWidth(), maxLines = 5
                            )
                        }
                    }

                    dialog.preguntasIA != null -> {
                        PreguntasIACard(preguntas = dialog.preguntasIA)
                        OutlinedTextField(
                            value = dialog.respuestaAclaracion,
                            onValueChange = onRespuestaAclaracionChanged,
                            label = { Text("Tu respuesta") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        if (dialog.parseando) {
                            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).size(24.dp))
                        }
                        dialog.errorIA?.let { err ->
                            OutlinedTextField(
                                value = err, onValueChange = {}, readOnly = true, isError = true,
                                label = { Text("Error IA", color = MaterialTheme.colorScheme.error) },
                                modifier = Modifier.fillMaxWidth(), maxLines = 5
                            )
                        }
                    }

                    else -> {
                        if (dialog.entradaEditando == null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = dialog.modoPlantilla,
                                    onClick = { onModoPlantillaChanged(true) },
                                    label = { Text("Plantilla") }
                                )
                                FilterChip(
                                    selected = !dialog.modoPlantilla,
                                    onClick = { onModoPlantillaChanged(false) },
                                    label = { Text("Libre") }
                                )
                            }
                        }

                        if (dialog.modoPlantilla && dialog.entradaEditando == null) {
                            PlantillaDropdown(
                                seleccionada = dialog.plantillaSeleccionada,
                                opciones = plantillas,
                                onSeleccion = onPlantillaSeleccionada
                            )
                        }

                        OutlinedTextField(
                            value = dialog.textoLibre,
                            onValueChange = onTextoLibreChanged,
                            label = { Text("Descripción") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!dialog.modoPlantilla && dialog.entradaEditando == null) {
                            Button(
                                onClick = onParsearConIA,
                                enabled = dialog.textoLibre.isNotBlank() && !dialog.parseando,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (dialog.parseando) SpinnerConTexto("Calculando...")
                                else Text("Calcular con IA")
                            }
                            dialog.errorIA?.let { err ->
                                OutlinedTextField(
                                    value = err, onValueChange = {}, readOnly = true, isError = true,
                                    label = { Text("Error IA", color = MaterialTheme.colorScheme.error) },
                                    modifier = Modifier.fillMaxWidth(), maxLines = 5
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = dialog.kcal, onValueChange = onKcalChanged,
                                label = { Text("kcal") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = dialog.proteinaG, onValueChange = onProteinaChanged,
                                label = { Text("Prot g") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = dialog.grasaG, onValueChange = onGrasaChanged,
                                label = { Text("Grasa g") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = dialog.carboG, onValueChange = onCarboChanged,
                                label = { Text("Carbo g") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                dialog.resultadoIA != null ->
                    TextButton(onClick = onAceptarYGuardar) { Text("Aceptar") }
                dialog.preguntasIA != null ->
                    TextButton(
                        onClick = onEnviarAclaracion,
                        enabled = dialog.respuestaAclaracion.isNotBlank() && !dialog.parseando
                    ) { Text("Responder") }
                else ->
                    TextButton(onClick = onGuardar, enabled = puedoGuardar) { Text("Guardar") }
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ResultadoIACard(resultado: ResultadoIAState, onEditarAMano: () -> Unit) {
    val confianzaColor = when (resultado.confianza) {
        "alta" -> Color(0xFF388E3C)
        "baja" -> MaterialTheme.colorScheme.error
        else -> Color(0xFFF57C00)
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${resultado.kcal} kcal", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Confianza: ${resultado.confianza}",
                    style = MaterialTheme.typography.labelSmall,
                    color = confianzaColor
                )
            }
            Text(
                "${resultado.proteinaG.toInt()}g prot · ${resultado.grasaG.toInt()}g grasa · ${resultado.carboG.toInt()}g carbo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            resultado.supuestos?.let { sup ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                Text(
                    "Asumido: $sup",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(
                onClick = onEditarAMano,
                modifier = Modifier.align(Alignment.End)
            ) { Text("Editar a mano") }
        }
    }
}

@Composable
private fun PreguntasIACard(preguntas: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "La IA necesita más información:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            preguntas.forEach { pregunta ->
                Text(
                    "• $pregunta",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun SpinnerConTexto(texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
        Text(texto)
    }
}

@Composable
private fun PlantillaDropdown(
    seleccionada: ComidaBase?,
    opciones: List<ComidaBase>,
    onSeleccion: (ComidaBase) -> Unit
) {
    var expandido by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = seleccionada?.variante ?: "Elegir variante",
            onValueChange = {},
            readOnly = true,
            label = { Text("Plantilla") },
            trailingIcon = {
                Icon(
                    if (expandido) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Box(modifier = Modifier.matchParentSize().clickable { expandido = !expandido })
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            if (opciones.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Sin plantillas para este slot", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    onClick = { expandido = false }
                )
            } else {
                opciones.forEach { comida ->
                    DropdownMenuItem(
                        text = { Text("${comida.variante} · ${comida.kcal} kcal") },
                        onClick = {
                            onSeleccion(comida)
                            expandido = false
                        }
                    )
                }
            }
        }
    }
}

private fun SlotComida.etiqueta() = when (this) {
    SlotComida.DESAYUNO -> "Desayuno"
    SlotComida.ALMUERZO -> "Almuerzo"
    SlotComida.COMIDA -> "Comida"
    SlotComida.MERIENDA -> "Merienda"
    SlotComida.CENA -> "Cena"
}
