package com.vic.recompo.ui.wizard

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.vic.recompo.domain.model.Sexo

@Composable
fun WizardScreen(viewModel: WizardViewModel) {
    val state by viewModel.state.collectAsState()

    if (state.isSaving) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(48.dp))

        LinearProgressIndicator(
            progress = { (state.step + 1) / 3f },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Paso ${state.step + 1} de 3",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(Modifier.height(24.dp))

        when (state.step) {
            0 -> Step1(state, viewModel)
            1 -> Step2(state, viewModel)
            2 -> Step3(state, viewModel)
        }

        state.error?.let { error ->
            Spacer(Modifier.height(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Step1(state: WizardState, vm: WizardViewModel) {
    Text("Datos personales", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(20.dp))

    OutlinedTextField(
        value = state.nombre,
        onValueChange = vm::onNombreChanged,
        label = { Text("Nombre") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
        value = state.fechaNacimiento,
        onValueChange = vm::onFechaNacimientoChanged,
        label = { Text("Fecha de nacimiento") },
        placeholder = { Text("AAAA-MM-DD") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))

    Text("Sexo biológico", style = MaterialTheme.typography.bodyMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = state.sexo == Sexo.HOMBRE,
            onClick = { vm.onSexoChanged(Sexo.HOMBRE) },
            label = { Text("Hombre") }
        )
        FilterChip(
            selected = state.sexo == Sexo.MUJER,
            onClick = { vm.onSexoChanged(Sexo.MUJER) },
            label = { Text("Mujer") }
        )
    }
    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
        value = state.alturaCm,
        onValueChange = vm::onAlturaCmChanged,
        label = { Text("Altura (cm)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(24.dp))

    Button(onClick = vm::onNext, modifier = Modifier.fillMaxWidth()) {
        Text("Siguiente")
    }
}

@Composable
private fun Step2(state: WizardState, vm: WizardViewModel) {
    Text("Plan y macros", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(20.dp))

    OutlinedTextField(
        value = state.fechaInicioPlan,
        onValueChange = vm::onFechaInicioPlanChanged,
        label = { Text("Inicio del plan") },
        placeholder = { Text("AAAA-MM-DD") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.pesoInicialKg,
            onValueChange = vm::onPesoInicialKgChanged,
            label = { Text("Peso inicial (kg)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = state.pesoObjetivoKg,
            onValueChange = vm::onPesoObjetivoKgChanged,
            label = { Text("Peso objetivo (kg)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
        value = state.faseActual,
        onValueChange = vm::onFaseActualChanged,
        label = { Text("Fase actual") },
        placeholder = { Text("Fase 1 casa") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
        value = state.kcalBaseDia,
        onValueChange = vm::onKcalBaseDiaChanged,
        label = { Text("Kcal base/día") },
        supportingText = { Text("Déficit de mantenimiento. La actividad registrada suma encima.") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
        value = state.proteinaObjetivoG,
        onValueChange = vm::onProteinaObjetivoGChanged,
        label = { Text("Proteína objetivo (g/día)") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = vm::onBack, modifier = Modifier.weight(1f)) {
            Text("Atrás")
        }
        Button(onClick = vm::onNext, modifier = Modifier.weight(1f)) {
            Text("Siguiente")
        }
    }
}

@Composable
private fun Step3(state: WizardState, vm: WizardViewModel) {
    val context = LocalContext.current

    val folderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            vm.onBackupUriSelected(it.toString())
        }
    }

    Text("Backup (opcional)", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text(
        "Selecciona una carpeta donde se guardarán copias de seguridad en JSON. Puedes configurarlo más tarde en Ajustes.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(20.dp))

    if (state.backupUri != null) {
        Text(
            "Carpeta: ${state.backupUri}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
    }

    OutlinedButton(
        onClick = { folderLauncher.launch(null) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (state.backupUri == null) "Seleccionar carpeta" else "Cambiar carpeta")
    }
    Spacer(Modifier.height(24.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = vm::onBack, modifier = Modifier.weight(1f)) {
            Text("Atrás")
        }
        Button(onClick = vm::onSaveAndFinish, modifier = Modifier.weight(1f)) {
            Text("Guardar")
        }
    }
    Spacer(Modifier.height(8.dp))
    TextButton(onClick = vm::onSkipBackup, modifier = Modifier.fillMaxWidth()) {
        Text("Omitir por ahora")
    }
}
