package com.vic.recompo.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vic.recompo.domain.model.Sexo
import com.vic.recompo.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserSettingsStore(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val KEY_SETUP_DONE = booleanPreferencesKey("setup_done")
        val KEY_NOMBRE = stringPreferencesKey("nombre")
        val KEY_FECHA_NACIMIENTO = stringPreferencesKey("fecha_nacimiento")
        val KEY_SEXO = stringPreferencesKey("sexo")
        val KEY_ALTURA_CM = intPreferencesKey("altura_cm")
        val KEY_FECHA_INICIO_PLAN = stringPreferencesKey("fecha_inicio_plan")
        val KEY_PESO_INICIAL_KG = doublePreferencesKey("peso_inicial_kg")
        val KEY_PESO_OBJETIVO_KG = doublePreferencesKey("peso_objetivo_kg")
        val KEY_FASE_ACTUAL = stringPreferencesKey("fase_actual")
        val KEY_KCAL_DESCANSO = intPreferencesKey("kcal_descanso")
        val KEY_KCAL_MUSCULACION = intPreferencesKey("kcal_musculacion")
        val KEY_KCAL_BICI = intPreferencesKey("kcal_bici")
        val KEY_PROTEINA_OBJETIVO_G = intPreferencesKey("proteina_objetivo_g")
        val KEY_CARPETA_BACKUP_URI = stringPreferencesKey("carpeta_backup_uri")
        val KEY_ULTIMO_BACKUP_OK = longPreferencesKey("ultimo_backup_ok")
        val KEY_ULTIMO_BACKUP_ERROR = stringPreferencesKey("ultimo_backup_error")
        val KEY_ULTIMO_BACKUP_BYTES = longPreferencesKey("ultimo_backup_bytes")
    }

    val setupDone: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SETUP_DONE] ?: false
    }

    val settings: Flow<UserSettings?> = dataStore.data.map { prefs ->
        val nombre = prefs[KEY_NOMBRE] ?: return@map null
        val fechaNacimiento = prefs[KEY_FECHA_NACIMIENTO]
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@map null
        val sexo = prefs[KEY_SEXO]
            ?.let { runCatching { Sexo.valueOf(it) }.getOrNull() } ?: return@map null
        val alturaCm = prefs[KEY_ALTURA_CM] ?: return@map null
        val fechaInicioPlan = prefs[KEY_FECHA_INICIO_PLAN]
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@map null
        val pesoInicialKg = prefs[KEY_PESO_INICIAL_KG] ?: return@map null
        val pesoObjetivoKg = prefs[KEY_PESO_OBJETIVO_KG] ?: return@map null
        val faseActual = prefs[KEY_FASE_ACTUAL] ?: return@map null
        val kcalDescanso = prefs[KEY_KCAL_DESCANSO] ?: return@map null
        val kcalMusculacion = prefs[KEY_KCAL_MUSCULACION] ?: return@map null
        val kcalBici = prefs[KEY_KCAL_BICI] ?: return@map null
        val proteinaObjetivoG = prefs[KEY_PROTEINA_OBJETIVO_G] ?: return@map null
        UserSettings(
            nombre = nombre,
            fechaNacimiento = fechaNacimiento,
            sexo = sexo,
            alturaCm = alturaCm,
            fechaInicioPlan = fechaInicioPlan,
            pesoInicialKg = pesoInicialKg,
            pesoObjetivoKg = pesoObjetivoKg,
            faseActual = faseActual,
            kcalDescanso = kcalDescanso,
            kcalMusculacion = kcalMusculacion,
            kcalBici = kcalBici,
            proteinaObjetivoG = proteinaObjetivoG,
            carpetaBackupUri = prefs[KEY_CARPETA_BACKUP_URI],
            ultimoBackupOk = prefs[KEY_ULTIMO_BACKUP_OK]?.let { Instant.ofEpochMilli(it) },
            ultimoBackupError = prefs[KEY_ULTIMO_BACKUP_ERROR],
            ultimoBackupBytes = prefs[KEY_ULTIMO_BACKUP_BYTES]
        )
    }

    suspend fun save(s: UserSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_NOMBRE] = s.nombre
            prefs[KEY_FECHA_NACIMIENTO] = s.fechaNacimiento.toString()
            prefs[KEY_SEXO] = s.sexo.name
            prefs[KEY_ALTURA_CM] = s.alturaCm
            prefs[KEY_FECHA_INICIO_PLAN] = s.fechaInicioPlan.toString()
            prefs[KEY_PESO_INICIAL_KG] = s.pesoInicialKg
            prefs[KEY_PESO_OBJETIVO_KG] = s.pesoObjetivoKg
            prefs[KEY_FASE_ACTUAL] = s.faseActual
            prefs[KEY_KCAL_DESCANSO] = s.kcalDescanso
            prefs[KEY_KCAL_MUSCULACION] = s.kcalMusculacion
            prefs[KEY_KCAL_BICI] = s.kcalBici
            prefs[KEY_PROTEINA_OBJETIVO_G] = s.proteinaObjetivoG
            s.carpetaBackupUri?.let { prefs[KEY_CARPETA_BACKUP_URI] = it }
            s.ultimoBackupOk?.let { prefs[KEY_ULTIMO_BACKUP_OK] = it.toEpochMilli() }
            s.ultimoBackupError?.let { prefs[KEY_ULTIMO_BACKUP_ERROR] = it }
            s.ultimoBackupBytes?.let { prefs[KEY_ULTIMO_BACKUP_BYTES] = it }
        }
    }

    suspend fun markSetupDone() {
        dataStore.edit { prefs -> prefs[KEY_SETUP_DONE] = true }
    }

    suspend fun saveBackupUri(uri: String) {
        dataStore.edit { prefs -> prefs[KEY_CARPETA_BACKUP_URI] = uri }
    }
}
