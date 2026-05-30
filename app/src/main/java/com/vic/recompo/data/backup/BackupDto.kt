package com.vic.recompo.data.backup

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 2,
    val exportadoEn: String,
    val settings: SettingsDto?,
    val mediciones: List<MedicionDto>,
    val entradasComida: List<EntradaComidaDto>,
    val actividades: List<ActividadDto>,
    val tiposSesion: List<TipoSesionDto>,
    val sesiones: List<SesionDto>,
    val ejerciciosEnSesion: List<EjercicioEnSesionDto>,
    val series: List<SerieDto>,
    val ejercicios: List<EjercicioDto>,
    val comidasBase: List<ComidaBaseDto>
)

@Serializable
data class SettingsDto(
    val nombre: String,
    val fechaNacimiento: String,
    val sexo: String,
    val alturaCm: Int,
    val fechaInicioPlan: String,
    val pesoInicialKg: Double,
    val pesoObjetivoKg: Double,
    val faseActual: String,
    val kcalDescanso: Int,
    val kcalMusculacion: Int,
    val kcalBici: Int,
    val metabolismoBasalKcal: Int,
    val proteinaObjetivoG: Int
)

@Serializable
data class MedicionDto(
    val id: Long,
    val fecha: String,
    val pesoKg: Double,
    val cinturaCm: Double?,
    val caderaCm: Double?,
    val cuelloCm: Double?,
    val pechoCm: Double?,
    val bicepsCm: Double?,
    val musloCm: Double?,
    val alturaCmEnLaMedicion: Int,
    val grasaPct: Double?,
    val grasaPctOverride: Boolean,
    val masaGrasaKg: Double?,
    val masaMagraKg: Double?,
    val imc: Double?,
    val whr: Double?,
    val faseTexto: String?,
    val hito: String?,
    val notas: String?
)

@Serializable
data class EntradaComidaDto(
    val id: Long,
    val fecha: String,
    val slot: String,
    val textoLibre: String,
    val kcal: Int,
    val proteinaG: Double,
    val grasaG: Double,
    val carboG: Double,
    val comidaBaseId: Long?,
    val parseadaPorIA: Boolean,
    val timestamp: String
)

@Serializable
data class ActividadDto(
    val id: Long,
    val fecha: String,
    val tipo: String,
    val descripcion: String?,
    val duracionMin: Int?,
    val kcalQuemadas: Int
)

@Serializable
data class TipoSesionDto(
    val id: Long,
    val nombre: String,
    val descripcion: String?,
    val esSeed: Boolean,
    val activo: Boolean
)

@Serializable
data class SesionDto(
    val id: Long,
    val tipoSesionId: Long,
    val fechaEjecutada: String?,
    val estado: String,
    val generadaPor: String,
    val notasIA: String?,
    val notasGlobales: String?,
    val rirGlobal: Int?
)

@Serializable
data class EjercicioEnSesionDto(
    val id: Long,
    val sesionId: Long,
    val ejercicioId: Long,
    val orden: Int,
    val seriesObjetivo: Int,
    val repsObjetivoMin: Int,
    val repsObjetivoMax: Int,
    val cargaObjetivoKg: Double?,
    val notas: String?,
    val rir: Int?
)

@Serializable
data class SerieDto(
    val id: Long,
    val ejercicioEnSesionId: Long,
    val numero: Int,
    val repsReales: Int?,
    val cargaKg: Double?,
    val rir: Int?,
    val estado: String,
    val motivoOmision: String?
)

@Serializable
data class EjercicioDto(
    val id: Long,
    val nombre: String,
    val grupoMuscularPrincipal: String,
    val gruposSecundarios: String,
    val patron: String,
    val equipamientoCasa: Boolean,
    val equipamientoGym: Boolean,
    val notasTecnica: String?,
    val activo: Boolean
)

@Serializable
data class ComidaBaseDto(
    val id: Long,
    val slot: String,
    val variante: String,
    val kcal: Int,
    val proteinaG: Double,
    val grasaG: Double,
    val carboG: Double,
    val ingredientesTexto: String,
    val activo: Boolean
)
