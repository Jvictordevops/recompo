package com.vic.recompo

import android.app.Application
import com.vic.recompo.BuildConfig
import com.vic.recompo.data.UserSettingsStore
import com.vic.recompo.data.ai.ClaudeClient
import com.vic.recompo.data.db.RecompoDatabase
import com.vic.recompo.data.db.entity.ComidaBase
import com.vic.recompo.data.db.entity.Ejercicio
import com.vic.recompo.data.db.entity.EjercicioEnSesion
import com.vic.recompo.data.db.entity.Serie
import com.vic.recompo.data.db.entity.Sesion
import com.vic.recompo.data.db.entity.TipoSesion
import com.vic.recompo.domain.model.EstadoSerie
import com.vic.recompo.domain.model.EstadoSesion
import com.vic.recompo.domain.model.GrupoMuscular
import com.vic.recompo.domain.model.MotivoOmision
import com.vic.recompo.domain.model.OrigenSesion
import com.vic.recompo.domain.model.PatronMovimiento
import com.vic.recompo.domain.model.SlotComida
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class App : Application() {
    val database by lazy { RecompoDatabase.getInstance(this) }
    val userSettingsStore by lazy { UserSettingsStore(this) }
    val claudeApi by lazy { ClaudeClient.create(BuildConfig.CLAUDE_API_KEY, BuildConfig.DEBUG) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            seedTipoSesionIfEmpty()
            seedEjerciciosIfEmpty()
            seedEjerciciosGymFase2()
            seedSesionesHistoricoIfNeeded()
            recoverMissingSeedSesiones()
        }
        appScope.launch { seedComidasBaseIfEmpty() }
    }

    private suspend fun seedTipoSesionIfEmpty() {
        if (database.tipoSesionDao().count() > 0) return
        listOf(
            TipoSesion(nombre = "A", descripcion = "Tren superior", esSeed = true),
            TipoSesion(nombre = "B", descripcion = "Tren inferior", esSeed = true),
            TipoSesion(nombre = "C", descripcion = "Full body", esSeed = true),
        ).forEach { database.tipoSesionDao().insert(it) }
    }

    private suspend fun seedComidasBaseIfEmpty() {
        if (database.comidaBaseDao().count() > 0) return
        listOf(
            ComidaBase(slot = SlotComida.DESAYUNO, variante = "Leche+avena+whey", kcal = 374, proteinaG = 38.0, grasaG = 8.0, carboG = 38.0, ingredientesTexto = "250ml leche semi + 40g avena + 30g HSN whey", activo = true),
            ComidaBase(slot = SlotComida.ALMUERZO, variante = "Tostada atún", kcal = 244, proteinaG = 18.0, grasaG = 7.0, carboG = 26.0, ingredientesTexto = "60g pan integral + 50g atún aceite escurrido", activo = true),
            ComidaBase(slot = SlotComida.ALMUERZO, variante = "Tostada lomo", kcal = 319, proteinaG = 27.0, grasaG = 11.0, carboG = 26.0, ingredientesTexto = "60g pan integral + 50g lomo embuchado", activo = true),
            ComidaBase(slot = SlotComida.ALMUERZO, variante = "Tostada pavo", kcal = 184, proteinaG = 13.0, grasaG = 2.0, carboG = 26.0, ingredientesTexto = "60g pan integral + 40g pavo", activo = true),
            ComidaBase(slot = SlotComida.MERIENDA, variante = "Natillas", kcal = 118, proteinaG = 12.0, grasaG = 2.0, carboG = 13.0, ingredientesTexto = "120g natillas proteicas Hacendado", activo = true),
            ComidaBase(slot = SlotComida.MERIENDA, variante = "Natillas + plátano", kcal = 207, proteinaG = 13.0, grasaG = 2.0, carboG = 36.0, ingredientesTexto = "120g natillas proteicas Hacendado + 100g plátano", activo = true),
            ComidaBase(slot = SlotComida.MERIENDA, variante = "Natillas + plátano + uvas", kcal = 242, proteinaG = 13.0, grasaG = 2.0, carboG = 45.0, ingredientesTexto = "120g natillas proteicas Hacendado + 100g plátano + 50g uvas", activo = true),
        ).forEach { database.comidaBaseDao().insert(it) }
    }

    private suspend fun seedEjerciciosIfEmpty() {
        if (database.ejercicioDao().count() > 0) return
        val json = assets.open("seed/ejercicios.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        val entities = (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Ejercicio(
                nombre = obj.getString("nombre"),
                grupoMuscularPrincipal = GrupoMuscular.valueOf(obj.getString("grupoMuscularPrincipal")),
                gruposSecundarios = obj.getString("gruposSecundarios"),
                patron = PatronMovimiento.valueOf(obj.getString("patron")),
                equipamientoCasa = obj.getBoolean("equipamientoCasa"),
                equipamientoGym = obj.getBoolean("equipamientoGym"),
                notasTecnica = obj.optString("notasTecnica").takeIf { it.isNotEmpty() },
                activo = true
            )
        }
        database.ejercicioDao().insertAll(entities)
    }

    private suspend fun seedEjerciciosGymFase2() {
        val json = assets.open("seed/ejercicios_gym_fase2.json").bufferedReader().use { it.readText() }
        val array = JSONArray(json)
        val dao = database.ejercicioDao()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val nombre = obj.getString("nombre")
            if (dao.getByNombre(nombre) == null) {
                dao.insert(
                    Ejercicio(
                        nombre = nombre,
                        grupoMuscularPrincipal = GrupoMuscular.valueOf(obj.getString("grupoMuscularPrincipal")),
                        gruposSecundarios = obj.getString("gruposSecundarios"),
                        patron = PatronMovimiento.valueOf(obj.getString("patron")),
                        equipamientoCasa = obj.getBoolean("equipamientoCasa"),
                        equipamientoGym = obj.getBoolean("equipamientoGym"),
                        notasTecnica = obj.optString("notasTecnica").takeIf { it.isNotEmpty() },
                        activo = true,
                        seedId = obj.optString("seedId").takeIf { it.isNotEmpty() }
                    )
                )
            }
        }
    }

    private suspend fun seedSesionesHistoricoIfNeeded() {
        if (userSettingsStore.sesionesHistoricoSeeded.first()) return

        val json = assets.open("seed/sesiones_seed.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val sesionesArray = root.getJSONArray("sesiones")

        val tipoByNombre = database.tipoSesionDao().getAll().first().associateBy { it.nombre }
        val ejercicioDao = database.ejercicioDao()
        val sesionDao = database.sesionDao()
        val ejercicioEnSesionDao = database.ejercicioEnSesionDao()
        val serieDao = database.serieDao()

        for (i in 0 until sesionesArray.length()) {
            val s = sesionesArray.getJSONObject(i)
            val tipoNombre = s.getString("tipo")
            val tipo = tipoByNombre[tipoNombre]
            if (tipo == null) {
                Timber.w("seedSesiones: tipo '$tipoNombre' no encontrado, sesión $i saltada")
                continue
            }

            val fechaStr = if (s.isNull("fechaEjecutada")) null else s.getString("fechaEjecutada")
            val fechaInstant: Instant? = fechaStr?.let {
                LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant()
            }

            val sesionId = sesionDao.insert(
                Sesion(
                    tipoSesionId = tipo.id,
                    fechaEjecutada = fechaInstant,
                    estado = EstadoSesion.valueOf(s.getString("estado")),
                    generadaPor = OrigenSesion.valueOf(s.getString("generadaPor")),
                    notasIA = if (s.isNull("notasIA")) null else s.optString("notasIA").takeIf { it.isNotEmpty() },
                    notasGlobales = if (s.isNull("notasGlobales")) null else s.optString("notasGlobales").takeIf { it.isNotEmpty() },
                    rirGlobal = if (s.isNull("rirGlobal")) null else s.optInt("rirGlobal"),
                    seedId = s.optString("seedId").takeIf { it.isNotEmpty() }
                )
            )

            val ejerciciosArray = s.getJSONArray("ejercicios")
            for (j in 0 until ejerciciosArray.length()) {
                val e = ejerciciosArray.getJSONObject(j)
                val ejercNombre = e.getString("ejercicio")
                val ejercicio = ejercicioDao.getByNombre(ejercNombre)
                if (ejercicio == null) {
                    Timber.w("seedSesiones: ejercicio '$ejercNombre' no encontrado, entrada saltada")
                    continue
                }

                val cargaObjetivo = if (e.isNull("cargaObjetivoKg")) null else e.optDouble("cargaObjetivoKg").takeIf { !it.isNaN() }
                val eesId = ejercicioEnSesionDao.insert(
                    EjercicioEnSesion(
                        sesionId = sesionId,
                        ejercicioId = ejercicio.id,
                        orden = j + 1,
                        seriesObjetivo = e.getInt("seriesObjetivo"),
                        repsObjetivoMin = e.getInt("repsObjetivoMin"),
                        repsObjetivoMax = e.getInt("repsObjetivoMax"),
                        cargaObjetivoKg = cargaObjetivo,
                        notas = e.optString("notas").takeIf { it.isNotEmpty() }
                    )
                )

                val seriesArray = e.getJSONArray("series")
                for (k in 0 until seriesArray.length()) {
                    val sr = seriesArray.getJSONObject(k)
                    val estadoStr = sr.optString("estado", "COMPLETADA")
                    val estado = EstadoSerie.valueOf(estadoStr)
                    val motivoStr = sr.optString("motivoOmision", "")
                    val motivo = if (motivoStr.isNotEmpty()) MotivoOmision.valueOf(motivoStr) else null
                    val cargaRaw = sr.optDouble("carga", Double.NaN)
                    val cargaKg: Double? = if (cargaRaw.isNaN() || cargaRaw == 0.0) null else cargaRaw
                    serieDao.insert(
                        Serie(
                            ejercicioEnSesionId = eesId,
                            numero = k + 1,
                            repsReales = if (estado == EstadoSerie.OMITIDA) null else sr.optInt("reps"),
                            cargaKg = cargaKg,
                            rir = if (estado == EstadoSerie.OMITIDA) null else sr.optInt("rir"),
                            estado = estado,
                            motivoOmision = motivo
                        )
                    )
                }
            }
        }

        userSettingsStore.markSesionesHistoricoSeeded()
    }

    private suspend fun recoverMissingSeedSesiones() {
        val json = assets.open("seed/sesiones_seed.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val sesionesArray = root.getJSONArray("sesiones")
        val tipoByNombre = database.tipoSesionDao().getAll().first().associateBy { it.nombre }
        val sesionDao = database.sesionDao()

        for (i in 0 until sesionesArray.length()) {
            val s = sesionesArray.getJSONObject(i)
            if (s.getString("estado") != "PREPARADA") continue
            val seedId = s.optString("seedId").takeIf { it.isNotEmpty() } ?: continue
            val tipo = tipoByNombre[s.getString("tipo")] ?: continue

            // Check any record (activo or not) with this seedId
            val any = sesionDao.getBySeedIdAny(seedId)
            if (any != null) {
                when {
                    !any.activo ->
                        // soft-deleted → reactivate as PREPARADA
                        sesionDao.update(any.copy(activo = true, estado = EstadoSesion.PREPARADA, fechaEjecutada = null))
                    any.estado == EstadoSesion.OMITIDA ->
                        // accidentally canceled → revert to PREPARADA
                        sesionDao.update(any.copy(estado = EstadoSesion.PREPARADA, fechaEjecutada = null))
                    // else: already PREPARADA or EN_CURSO → skip
                }
                continue
            }
            val existePreparada = sesionDao.getPreparadaByTipo(tipo.id)
            if (existePreparada != null) {
                sesionDao.update(existePreparada.copy(seedId = seedId))
                continue
            }
            insertarSeedSesion(s, tipo)
        }
    }

    private suspend fun insertarSeedSesion(s: JSONObject, tipo: TipoSesion) {
        val fechaStr = if (s.isNull("fechaEjecutada")) null else s.getString("fechaEjecutada")
        val fechaInstant: Instant? = fechaStr?.let {
            LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant()
        }

        val sesionId = database.sesionDao().insert(
            Sesion(
                tipoSesionId = tipo.id,
                fechaEjecutada = fechaInstant,
                estado = EstadoSesion.valueOf(s.getString("estado")),
                generadaPor = OrigenSesion.valueOf(s.getString("generadaPor")),
                notasIA = if (s.isNull("notasIA")) null else s.optString("notasIA").takeIf { it.isNotEmpty() },
                notasGlobales = if (s.isNull("notasGlobales")) null else s.optString("notasGlobales").takeIf { it.isNotEmpty() },
                rirGlobal = if (s.isNull("rirGlobal")) null else s.optInt("rirGlobal"),
                seedId = s.optString("seedId").takeIf { it.isNotEmpty() }
            )
        )

        val ejerciciosArray = s.getJSONArray("ejercicios")
        for (j in 0 until ejerciciosArray.length()) {
            val e = ejerciciosArray.getJSONObject(j)
            val ejercNombre = e.getString("ejercicio")
            val ejercicio = database.ejercicioDao().getByNombre(ejercNombre)
            if (ejercicio == null) {
                Timber.w("insertarSeedSesion: ejercicio '$ejercNombre' no encontrado, saltada")
                continue
            }

            val cargaObjetivo = if (e.isNull("cargaObjetivoKg")) null else e.optDouble("cargaObjetivoKg").takeIf { !it.isNaN() }
            val eesId = database.ejercicioEnSesionDao().insert(
                EjercicioEnSesion(
                    sesionId = sesionId,
                    ejercicioId = ejercicio.id,
                    orden = j + 1,
                    seriesObjetivo = e.getInt("seriesObjetivo"),
                    repsObjetivoMin = e.getInt("repsObjetivoMin"),
                    repsObjetivoMax = e.getInt("repsObjetivoMax"),
                    cargaObjetivoKg = cargaObjetivo,
                    notas = e.optString("notas").takeIf { it.isNotEmpty() }
                )
            )

            val seriesArray = e.getJSONArray("series")
            for (k in 0 until seriesArray.length()) {
                val sr = seriesArray.getJSONObject(k)
                val estado = EstadoSerie.valueOf(sr.optString("estado", "COMPLETADA"))
                val motivoStr = sr.optString("motivoOmision", "")
                val motivo = if (motivoStr.isNotEmpty()) MotivoOmision.valueOf(motivoStr) else null
                val cargaRaw = sr.optDouble("carga", Double.NaN)
                val cargaKg: Double? = if (cargaRaw.isNaN() || cargaRaw == 0.0) null else cargaRaw
                database.serieDao().insert(
                    Serie(
                        ejercicioEnSesionId = eesId,
                        numero = k + 1,
                        repsReales = if (estado == EstadoSerie.OMITIDA) null else sr.optInt("reps"),
                        cargaKg = cargaKg,
                        rir = if (estado == EstadoSerie.OMITIDA) null else sr.optInt("rir"),
                        estado = estado,
                        motivoOmision = motivo
                    )
                )
            }
        }
    }
}
