package com.vic.recompo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vic.recompo.data.db.dao.ActividadDao
import com.vic.recompo.data.db.dao.ComidaBaseDao
import com.vic.recompo.data.db.dao.ConversacionDao
import com.vic.recompo.data.db.dao.EjercicioDao
import com.vic.recompo.data.db.dao.EjercicioEnSesionDao
import com.vic.recompo.data.db.dao.EntradaComidaDao
import com.vic.recompo.data.db.dao.MedicionDao
import com.vic.recompo.data.db.dao.MensajeIADao
import com.vic.recompo.data.db.dao.SerieDao
import com.vic.recompo.data.db.dao.SesionDao
import com.vic.recompo.data.db.entity.Actividad
import com.vic.recompo.data.db.entity.ComidaBase
import com.vic.recompo.data.db.entity.Conversacion
import com.vic.recompo.data.db.entity.Ejercicio
import com.vic.recompo.data.db.entity.EjercicioEnSesion
import com.vic.recompo.data.db.entity.EntradaComida
import com.vic.recompo.data.db.entity.Medicion
import com.vic.recompo.data.db.entity.MensajeIA
import com.vic.recompo.data.db.entity.Serie
import com.vic.recompo.data.db.entity.Sesion

@Database(
    entities = [
        ComidaBase::class,
        Ejercicio::class,
        Sesion::class,
        EjercicioEnSesion::class,
        Serie::class,
        EntradaComida::class,
        Actividad::class,
        Medicion::class,
        Conversacion::class,
        MensajeIA::class,
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RecompoDatabase : RoomDatabase() {

    abstract fun comidaBaseDao(): ComidaBaseDao
    abstract fun ejercicioDao(): EjercicioDao
    abstract fun sesionDao(): SesionDao
    abstract fun ejercicioEnSesionDao(): EjercicioEnSesionDao
    abstract fun serieDao(): SerieDao
    abstract fun entradaComidaDao(): EntradaComidaDao
    abstract fun actividadDao(): ActividadDao
    abstract fun medicionDao(): MedicionDao
    abstract fun conversacionDao(): ConversacionDao
    abstract fun mensajeIADao(): MensajeIADao

    companion object {
        @Volatile private var INSTANCE: RecompoDatabase? = null

        fun getInstance(context: Context): RecompoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, RecompoDatabase::class.java, "recompo.db")
                .fallbackToDestructiveMigration(true)
                .build()
    }
}
