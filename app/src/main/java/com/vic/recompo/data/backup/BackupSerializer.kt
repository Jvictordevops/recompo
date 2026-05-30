package com.vic.recompo.data.backup

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.vic.recompo.data.UserSettingsStore
import com.vic.recompo.data.db.RecompoDatabase
import com.vic.recompo.domain.model.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate

private val json = Json { prettyPrint = false; encodeDefaults = true }

class BackupSerializer(
    private val context: Context,
    private val database: RecompoDatabase,
    private val store: UserSettingsStore
) {

    suspend fun hacerBackup(): Result<Long> {
        val settings = store.settings.first()
        val uri = settings?.carpetaBackupUri ?: return Result.failure(
            IllegalStateException("Carpeta de backup no configurada")
        )
        return runCatching {
            val data = buildBackupData(settings)
            val encoded = json.encodeToString(data)
            val bytes = encoded.toByteArray(Charsets.UTF_8)
            val fecha = LocalDate.now()
            escribirEnCarpeta(uri, "recomposicion_$fecha.json", "application/json", bytes)
            val ahora = Instant.now()
            store.save(
                settings.copy(
                    ultimoBackupOk = ahora,
                    ultimoBackupError = null,
                    ultimoBackupBytes = bytes.size.toLong()
                )
            )
            bytes.size.toLong()
        }.onFailure { e ->
            Timber.e(e, "Backup falló")
            settings?.let {
                runCatching {
                    store.save(it.copy(ultimoBackupError = e.message ?: "Error desconocido"))
                }
            }
        }
    }

    suspend fun buildJsonBytes(settings: UserSettings): ByteArray {
        val data = buildBackupData(settings)
        return json.encodeToString(data).toByteArray(Charsets.UTF_8)
    }

    private suspend fun buildBackupData(settings: UserSettings): BackupData {
        val db = database
        return BackupData(
            exportadoEn = Instant.now().toString(),
            settings = settings.toDto(),
            mediciones = db.medicionDao().getAll().first().map { m ->
                MedicionDto(
                    id = m.id, fecha = m.fecha.toString(), pesoKg = m.pesoKg,
                    cinturaCm = m.cinturaCm, caderaCm = m.caderaCm, cuelloCm = m.cuelloCm,
                    pechoCm = m.pechoCm, bicepsCm = m.bicepsCm, musloCm = m.musloCm,
                    alturaCmEnLaMedicion = m.alturaCmEnLaMedicion, grasaPct = m.grasaPct,
                    grasaPctOverride = m.grasaPctOverride, masaGrasaKg = m.masaGrasaKg,
                    masaMagraKg = m.masaMagraKg, imc = m.imc, whr = m.whr,
                    faseTexto = m.faseTexto, hito = m.hito, notas = m.notas
                )
            },
            entradasComida = db.entradaComidaDao().getAll().map { e ->
                EntradaComidaDto(
                    id = e.id, fecha = e.fecha.toString(), slot = e.slot.name,
                    textoLibre = e.textoLibre, kcal = e.kcal, proteinaG = e.proteinaG,
                    grasaG = e.grasaG, carboG = e.carboG, comidaBaseId = e.comidaBaseId,
                    parseadaPorIA = e.parseadaPorIA, timestamp = e.timestamp.toString()
                )
            },
            actividades = db.actividadDao().getAll().first().map { a ->
                ActividadDto(
                    id = a.id, fecha = a.fecha.toString(), tipo = a.tipo,
                    descripcion = a.descripcion, duracionMin = a.duracionMin,
                    kcalQuemadas = a.kcalQuemadas
                )
            },
            tiposSesion = db.tipoSesionDao().getAll().first().map { t ->
                TipoSesionDto(
                    id = t.id, nombre = t.nombre, descripcion = t.descripcion,
                    esSeed = t.esSeed, activo = t.activo
                )
            },
            sesiones = db.sesionDao().getAll().first().map { s ->
                SesionDto(
                    id = s.id, tipoSesionId = s.tipoSesionId,
                    fechaEjecutada = s.fechaEjecutada?.toString(), estado = s.estado.name,
                    generadaPor = s.generadaPor.name, notasIA = s.notasIA,
                    notasGlobales = s.notasGlobales, rirGlobal = s.rirGlobal
                )
            },
            ejerciciosEnSesion = db.ejercicioEnSesionDao().getAll().map { e ->
                EjercicioEnSesionDto(
                    id = e.id, sesionId = e.sesionId, ejercicioId = e.ejercicioId,
                    orden = e.orden, seriesObjetivo = e.seriesObjetivo,
                    repsObjetivoMin = e.repsObjetivoMin, repsObjetivoMax = e.repsObjetivoMax,
                    cargaObjetivoKg = e.cargaObjetivoKg, notas = e.notas, rir = e.rir
                )
            },
            series = db.serieDao().getAll().map { s ->
                SerieDto(
                    id = s.id, ejercicioEnSesionId = s.ejercicioEnSesionId,
                    numero = s.numero, repsReales = s.repsReales, cargaKg = s.cargaKg,
                    rir = s.rir, estado = s.estado.name, motivoOmision = s.motivoOmision?.name
                )
            },
            ejercicios = db.ejercicioDao().getAll().first().map { e ->
                EjercicioDto(
                    id = e.id, nombre = e.nombre,
                    grupoMuscularPrincipal = e.grupoMuscularPrincipal.name,
                    gruposSecundarios = e.gruposSecundarios, patron = e.patron.name,
                    equipamientoCasa = e.equipamientoCasa, equipamientoGym = e.equipamientoGym,
                    notasTecnica = e.notasTecnica, activo = e.activo
                )
            },
            comidasBase = db.comidaBaseDao().getAll().first().map { c ->
                ComidaBaseDto(
                    id = c.id, slot = c.slot.name, variante = c.variante, kcal = c.kcal,
                    proteinaG = c.proteinaG, grasaG = c.grasaG, carboG = c.carboG,
                    ingredientesTexto = c.ingredientesTexto, activo = c.activo
                )
            }
        )
    }

    fun escribirEnCarpeta(folderUriStr: String, nombre: String, mime: String, bytes: ByteArray) {
        val folderUri = Uri.parse(folderUriStr)
        val folder = DocumentFile.fromTreeUri(context, folderUri)
            ?: error("URI de carpeta inválida")
        val existing = folder.findFile(nombre)
        val file = existing ?: folder.createFile(mime, nombre)
            ?: error("No se pudo crear $nombre en la carpeta de backup")
        context.contentResolver.openOutputStream(file.uri, "wt")?.use { out ->
            out.write(bytes)
        } ?: error("No se pudo abrir $nombre para escritura")
    }

    private fun UserSettings.toDto() = SettingsDto(
        nombre = nombre,
        fechaNacimiento = fechaNacimiento.toString(),
        sexo = sexo.name,
        alturaCm = alturaCm,
        fechaInicioPlan = fechaInicioPlan.toString(),
        pesoInicialKg = pesoInicialKg,
        pesoObjetivoKg = pesoObjetivoKg,
        faseActual = faseActual,
        kcalDescanso = kcalDescanso,
        kcalMusculacion = kcalMusculacion,
        kcalBici = kcalBici,
        metabolismoBasalKcal = metabolismoBasalKcal,
        proteinaObjetivoG = proteinaObjetivoG
    )
}
