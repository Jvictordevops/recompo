package com.vic.recompo.ui.home

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
fun HomeScreen(viewModel: HomeViewModel) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Encabezado(nombre = state.nombre, tipoDia = state.tipoDia) }
        item {
            TarjetaMacros(
                kcalConsumidas = state.kcalConsumidas,
                kcalObjetivo = state.kcalObjetivo,
                proteinaConsumidaG = state.proteinaConsumidaG,
                proteinaObjetivoG = state.proteinaObjetivoG
            )
        }
        state.sesionDelDia?.let { sesion ->
            item { TarjetaSesion(sesion = sesion) }
        }
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
private fun Encabezado(nombre: String, tipoDia: TipoDia) {
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
        TipoDiaChip(tipoDia)
    }
}

@Composable
private fun TipoDiaChip(tipoDia: TipoDia) {
    val (label, color) = when (tipoDia) {
        TipoDia.MUSCULACION -> "Musculación" to MaterialTheme.colorScheme.primaryContainer
        TipoDia.BICI -> "Bici" to MaterialTheme.colorScheme.secondaryContainer
        TipoDia.DESCANSO -> "Descanso" to MaterialTheme.colorScheme.surfaceVariant
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = color)
    )
}

@Composable
private fun TarjetaMacros(
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
private fun TarjetaSesion(sesion: Sesion) {
    val estadoTexto = when (sesion.estado) {
        EstadoSesion.PLANIFICADA -> "Planificada"
        EstadoSesion.EN_CURSO -> "En curso"
        else -> sesion.estado.name
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Sesión ${sesion.tipo}", style = MaterialTheme.typography.titleMedium)
            Text(estadoTexto, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
