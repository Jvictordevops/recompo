package com.vic.recompo.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
import com.vic.recompo.data.db.dao.TipoSesionDao
import com.vic.recompo.data.db.dao.UsoIADao
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
import com.vic.recompo.data.db.entity.TipoSesion
import com.vic.recompo.data.db.entity.UsoIA

@Database(
    entities = [
        TipoSesion::class,
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
        UsoIA::class,
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RecompoDatabase : RoomDatabase() {

    abstract fun tipoSesionDao(): TipoSesionDao
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
    abstract fun usoIADao(): UsoIADao

    companion object {
        @Volatile private var INSTANCE: RecompoDatabase? = null

        fun getInstance(context: Context): RecompoDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE Sesion ADD COLUMN seedId TEXT")
                db.execSQL("ALTER TABLE Sesion ADD COLUMN activo INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE Ejercicio ADD COLUMN seedId TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS UsoIA (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        funcion TEXT NOT NULL,
                        proveedor TEXT NOT NULL,
                        modelo TEXT NOT NULL,
                        tokensIn INTEGER NOT NULL,
                        tokensOut INTEGER NOT NULL,
                        costeUsd REAL NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_UsoIA_timestamp ON UsoIA(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_UsoIA_funcion ON UsoIA(funcion)")
            }
        }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(context, RecompoDatabase::class.java, "recompo.db")
                .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                .fallbackToDestructiveMigration(true)
                .build()
    }
}
