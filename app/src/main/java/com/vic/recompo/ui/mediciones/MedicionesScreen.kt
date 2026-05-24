package com.vic.recompo.ui.mediciones

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vic.recompo.data.db.entity.Medicion
import java.time.format.DateTimeFormatter

private val FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
fun MedicionesScreen(viewModel: MedicionesViewModel) {
    val state by viewModel.uiState.collectAsState()
    val form by viewModel.formState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.mediciones.isEmpty()) {
            Text(
                "Sin mediciones aún",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.mediciones, key = { it.id }) { m ->
                    MedicionCard(
                        medicion = m,
                        onClick = { viewModel.abrirFormulario(m) },
                        onBorrar = { viewModel.borrar(m.id) }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { viewModel.abrirFormulario() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Nueva medición")
        }
    }

    if (form.abierto) {
        MedicionDialog(
            form = form,
            onFechaChanged = viewModel::onFechaChanged,
            onPesoChanged = viewModel::onPesoChanged,
            onCinturaChanged = viewModel::onCinturaChanged,
            onCaderaChanged = viewModel::onCaderaChanged,
            onCuelloChanged = viewModel::onCuelloChanged,
            onPechoChanged = viewModel::onPechoChanged,
            onBicepsChanged = viewModel::onBicepsChanged,
            onMusloChanged = viewModel::onMusloChanged,
            onGrasaOverrideChanged = viewModel::onGrasaOverrideChanged,
            onGrasaManualChanged = viewModel::onGrasaManualChanged,
            onHitoChanged = viewModel::onHitoChanged,
            onNotasChanged = viewModel::onNotasChanged,
            onGuardar = viewModel::guardar,
            onCancelar = viewModel::cerrarFormulario
        )
    }
}

@Composable
private fun MedicionCard(medicion: Medicion, onClick: () -> Unit, onBorrar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(medicion.fecha.format(FMT), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    medicion.hito?.let {
                        Spacer(Modifier.width(8.dp))
                        Text("· $it", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("${medicion.pesoKg} kg", style = MaterialTheme.typography.titleMedium)
                val extras = buildList {
                    medicion.grasaPct?.let { add("%.1f%% grasa".format(it)) }
                    medicion.masaMagraKg?.let { add("%.1f kg magra".format(it)) }
                    medicion.imc?.let { add("IMC %.1f".format(it)) }
                }
                if (extras.isNotEmpty()) {
                    Text(
                        extras.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onBorrar) {
                Icon(Icons.Default.Delete, contentDescription = "Borrar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MedicionDialog(
    form: MedicionFormState,
    onFechaChanged: (String) -> Unit,
    onPesoChanged: (String) -> Unit,
    onCinturaChanged: (String) -> Unit,
    onCaderaChanged: (String) -> Unit,
    onCuelloChanged: (String) -> Unit,
    onPechoChanged: (String) -> Unit,
    onBicepsChanged: (String) -> Unit,
    onMusloChanged: (String) -> Unit,
    onGrasaOverrideChanged: (Boolean) -> Unit,
    onGrasaManualChanged: (String) -> Unit,
    onHitoChanged: (String) -> Unit,
    onNotasChanged: (String) -> Unit,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit
) {
    val titulo = if (form.medicionEditando != null) "Editar medición" else "Nueva medición"
    val puedeGuardar = form.pesoKg.toDoubleOrNull() != null &&
        runCatching { java.time.LocalDate.parse(form.fecha) }.isSuccess &&
        (!form.grasaPctOverride || form.grasaPctManual.toDoubleOrNull() != null)

    Dialog(onDismissRequest = onCancelar) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(titulo, style = MaterialTheme.typography.titleLarge)

                // Fecha y peso
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = form.fecha,
                        onValueChange = onFechaChanged,
                        label = { Text("Fecha") },
                        placeholder = { Text("AAAA-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = form.pesoKg,
                        onValueChange = onPesoChanged,
                        label = { Text("Peso kg") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider()
                Text("Medidas corporales (cm)", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CmField("Cintura", form.cinturaCm, onCinturaChanged, Modifier.weight(1f))
                    CmField("Cadera", form.caderaCm, onCaderaChanged, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CmField("Cuello", form.cuelloCm, onCuelloChanged, Modifier.weight(1f))
                    CmField("Pecho", form.pechoCm, onPechoChanged, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CmField("Bíceps", form.bicepsCm, onBicepsChanged, Modifier.weight(1f))
                    CmField("Muslo", form.musloCm, onMusloChanged, Modifier.weight(1f))
                }

                HorizontalDivider()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onGrasaOverrideChanged(!form.grasaPctOverride) }
                ) {
                    Checkbox(checked = form.grasaPctOverride, onCheckedChange = onGrasaOverrideChanged)
                    Text("Introducir % grasa manualmente", style = MaterialTheme.typography.bodyMedium)
                }
                if (form.grasaPctOverride) {
                    OutlinedTextField(
                        value = form.grasaPctManual,
                        onValueChange = onGrasaManualChanged,
                        label = { Text("% grasa") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider()
                OutlinedTextField(
                    value = form.hito,
                    onValueChange = onHitoChanged,
                    label = { Text("Hito (opcional)") },
                    placeholder = { Text("Inicio, -3kg, etc.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = form.notas,
                    onValueChange = onNotasChanged,
                    label = { Text("Notas (opcional)") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCancelar) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onGuardar, enabled = puedeGuardar) { Text("Guardar") }
                }
            }
        }
    }
}

@Composable
private fun CmField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}
