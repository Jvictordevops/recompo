package com.vic.recompo

import android.app.Application
import com.vic.recompo.BuildConfig
import com.vic.recompo.data.UserSettingsStore
import com.vic.recompo.data.ai.ClaudeClient
import com.vic.recompo.data.db.RecompoDatabase
import com.vic.recompo.data.db.entity.ComidaBase
import com.vic.recompo.data.db.entity.Ejercicio
import com.vic.recompo.domain.model.GrupoMuscular
import com.vic.recompo.domain.model.PatronMovimiento
import com.vic.recompo.domain.model.SlotComida
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray

class App : Application() {
    val database by lazy { RecompoDatabase.getInstance(this) }
    val userSettingsStore by lazy { UserSettingsStore(this) }
    val claudeApi by lazy { ClaudeClient.create(BuildConfig.CLAUDE_API_KEY, BuildConfig.DEBUG) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { seedEjerciciosIfEmpty() }
        appScope.launch { seedComidasBaseIfEmpty() }
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
}
