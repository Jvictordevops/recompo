package com.vic.recompo

import android.app.Application
import com.vic.recompo.data.UserSettingsStore
import com.vic.recompo.data.db.RecompoDatabase
import com.vic.recompo.data.db.entity.Ejercicio
import com.vic.recompo.domain.model.GrupoMuscular
import com.vic.recompo.domain.model.PatronMovimiento
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray

class App : Application() {
    val database by lazy { RecompoDatabase.getInstance(this) }
    val userSettingsStore by lazy { UserSettingsStore(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { seedEjerciciosIfEmpty() }
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
