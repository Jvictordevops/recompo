package com.vic.recompo.ui.home

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
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vic.recompo.data.db.entity.Sesion
import com.vic.recompo.domain.model.EstadoSesion
import com.vic.recompo.ui.common.BackupChip
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToEntreno: () -> Unit,
    onNavigateToChat: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Encabezado(
                nombre = state.nombre,
                tipoDia = state.tipoDia,
                onTipoDiaChanged = viewModel::setTipoDia
            )
        }
        item {
            TarjetaObjetivoNutricional(
                kcalConsumidas = state.kcalConsumidas,
                kcalObjetivo = state.kcalObjetivo,
                proteinaConsumidaG = state.proteinaConsumidaG,
                proteinaObjetivoG = state.proteinaObjetivoG
            )
        }
        item {
            TarjetaGasto(
                metabolismoBasal = state.metabolismoBasalKcal,
                kcalActividad = state.kcalActividad,
                kcalGastoReal = state.kcalGastoReal,
                kcalConsumidas = state.kcalConsumidas
            )
        }
        val sesionActiva = state.sesionActiva
        if (sesionActiva != null) {
            item {
                TarjetaSesion(
                    sesion = sesionActiva,
                    tipoNombre = state.sesionActivaTipoNombre
                )
            }
        } else {
            item { TarjetaIA(onNavigateToEntreno = onNavigateToEntreno) }
        }
        item { TarjetaChat(onNavigateToChat = onNavigateToChat) }
        item {
            BackupChip(
                backupUri = state.backupUri,
                ultimoBackupOk = state.ultimoBackupOk,
                ultimoBackupError = state.ultimoBackupError
            )
        }
    }
}

@Composable
private fun Encabezado(nombre: String, tipoDia: TipoDia, onTipoDiaChanged: (TipoDia) -> Unit) {
    val fecha = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, d MMM", Locale("es")))
        .replaceFirstChar { it.uppercase() }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            if (nombre.isNotEmpty()) {
                Text("Hola, $nombre", style = MaterialTheme.typography.titleLarge)
            }
            Text(fecha, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TipoDiaChip(tipoDia, onTipoDiaChanged)
    }
}

@Composable
private fun TipoDiaChip(tipoDia: TipoDia, onTipoDiaChanged: (TipoDia) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val (label, color) = when (tipoDia) {
        TipoDia.MUSCULACION -> "Musculación" to MaterialTheme.colorScheme.primaryContainer
        TipoDia.BICI -> "Bici" to MaterialTheme.colorScheme.secondaryContainer
        TipoDia.DESCANSO -> "Descanso" to MaterialTheme.colorScheme.surfaceVariant
    }
    Box {
        SuggestionChip(
            onClick = { expanded = true },
            label = { Text(label) },
            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = color)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            TipoDia.entries.forEach { tipo ->
                val itemLabel = when (tipo) {
                    TipoDia.MUSCULACION -> "Musculación"
                    TipoDia.BICI -> "Bici"
                    TipoDia.DESCANSO -> "Descanso"
                }
                DropdownMenuItem(
                    text = { Text(itemLabel) },
                    onClick = { onTipoDiaChanged(tipo); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun TarjetaObjetivoNutricional(
    kcalConsumidas: Int,
    kcalObjetivo: Int,
    proteinaConsumidaG: Double,
    proteinaObjetivoG: Int
) {
    val progresoKcal = if (kcalObjetivo > 0)
        (kcalConsumidas.toFloat() / kcalObjetivo).coerceIn(0f, 1f) else 0f
    val progresoProt = if (proteinaObjetivoG > 0)
        (proteinaConsumidaG.toFloat() / proteinaObjetivoG).coerceIn(0f, 1f) else 0f

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Objetivo nutricional", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            FilaMacro(
                etiqueta = "Calorías",
                consumido = "$kcalConsumidas",
                objetivo = "$kcalObjetivo kcal",
                progreso = progresoKcal
            )
            FilaMacro(
                etiqueta = "Proteína",
                consumido = "${proteinaConsumidaG.toInt()}g",
                objetivo = "${proteinaObjetivoG}g",
                progreso = progresoProt
            )
        }
    }
}

@Composable
private fun TarjetaGasto(
    metabolismoBasal: Int,
    kcalActividad: Int,
    kcalGastoReal: Int,
    kcalConsumidas: Int
) {
    val deficit = kcalConsumidas - kcalGastoReal

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Gasto estimado", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Metabolismo basal", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$metabolismoBasal kcal", style = MaterialTheme.typography.bodySmall)
            }
            if (kcalActividad > 0) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Actividad", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+$kcalActividad kcal", style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Gasto total", style = MaterialTheme.typography.bodyMedium)
                Text("$kcalGastoReal kcal", style = MaterialTheme.typography.bodyMedium)
            }
            if (kcalConsumidas > 0 || kcalGastoReal > 0) {
                Spacer(Modifier.height(2.dp))
                val deficitLabel = if (deficit <= 0) "Déficit: ${-deficit} kcal" else "Superávit: +$deficit kcal"
                val deficitColor = if (deficit <= 0) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.error
                Text(deficitLabel, style = MaterialTheme.typography.labelSmall, color = deficitColor)
            }
        }
    }
}

@Composable
private fun FilaMacro(etiqueta: String, consumido: String, objetivo: String, progreso: Float) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(etiqueta, style = MaterialTheme.typography.labelMedium)
            Text(
                "$consumido / $objetivo",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LinearProgressIndicator(
            progress = { progreso },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun TarjetaSesion(sesion: Sesion, tipoNombre: String?) {
    val estadoTexto = when (sesion.estado) {
        EstadoSesion.PREPARADA -> "Preparada"
        EstadoSesion.EN_CURSO -> "En curso"
        else -> sesion.estado.name
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Sesión ${tipoNombre ?: "?"}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                estadoTexto,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaChat(onNavigateToChat: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Asistente IA",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    "Consulta sobre tu dieta, progreso o entrenos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = onNavigateToChat) {
                Text("Chat")
            }
        }
    }
}

@Composable
private fun TarjetaIA(onNavigateToEntreno: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Próxima sesión",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                "Genera tu próxima sesión de entreno con IA.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(
                onClick = onNavigateToEntreno,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ir a Entrenos") }
        }
    }
}
