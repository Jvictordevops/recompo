package com.vic.recompo.ui.common

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import java.time.Duration
import java.time.Instant

@Composable
fun BackupChip(
    backupUri: String?,
    ultimoBackupOk: Instant?,
    ultimoBackupError: String?
) {
    val horasDesdeBackup = ultimoBackupOk?.let { Duration.between(it, Instant.now()).toHours() }

    val label = when {
        backupUri == null -> "Sin backup configurado"
        ultimoBackupError != null && ultimoBackupOk == null -> "Backup falló ⚠"
        ultimoBackupOk == null -> "Backup pendiente"
        horasDesdeBackup!! <= 48 -> "Backup hace ${horasDesdeBackup}h ✓"
        else -> "Backup hace ${horasDesdeBackup / 24}d ⚠"
    }
    val isWarning = backupUri != null && (
        (ultimoBackupError != null && ultimoBackupOk == null) ||
        (horasDesdeBackup != null && horasDesdeBackup > 48)
    )

    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = if (isWarning) AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            labelColor = MaterialTheme.colorScheme.onErrorContainer
        ) else AssistChipDefaults.assistChipColors()
    )
}
