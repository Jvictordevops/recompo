package com.vic.recompo.data.backup

import android.content.Context
import com.vic.recompo.data.UserSettingsStore
import com.vic.recompo.data.db.RecompoDatabase
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipExporter(
    private val context: Context,
    private val database: RecompoDatabase,
    private val store: UserSettingsStore,
    private val backupSerializer: BackupSerializer,
    private val xlsxExporter: XlsxExporter
) {

    suspend fun generarBytes(): ByteArray {
        val settings = store.settings.first()
            ?: error("Settings no cargados")
        val fecha = LocalDate.now().toString()

        val jsonBytes = backupSerializer.buildJsonBytes(settings)
        val xlsxBytes = xlsxExporter.generarBytes()
        val dbBytes = leerSqliteBytes()

        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("recomposicion.json"))
            zip.write(jsonBytes)
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("recomposicion_$fecha.xlsx"))
            zip.write(xlsxBytes)
            zip.closeEntry()

            zip.putNextEntry(ZipEntry("recomposicion_raw.db"))
            zip.write(dbBytes)
            zip.closeEntry()
        }
        Timber.d("ZIP generado: ${out.size()} bytes")
        return out.toByteArray()
    }

    private fun leerSqliteBytes(): ByteArray {
        // Checkpoint WAL antes de copiar para asegurar que el .db está al día
        runCatching {
            database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
        }
        val dbFile = context.getDatabasePath("recompo.db")
        return if (dbFile.exists()) dbFile.readBytes() else ByteArray(0)
    }
}
