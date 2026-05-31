package com.vic.recompo.data.db

import androidx.room.TypeConverter
import com.vic.recompo.domain.model.EstadoSerie
import com.vic.recompo.domain.model.EstadoSesion
import com.vic.recompo.domain.model.FuncionIA
import com.vic.recompo.domain.model.GrupoMuscular
import com.vic.recompo.domain.model.MotivoOmision
import com.vic.recompo.domain.model.OrigenSesion
import com.vic.recompo.domain.model.PatronMovimiento
import com.vic.recompo.domain.model.ProveedorIA
import com.vic.recompo.domain.model.RolMensaje
import com.vic.recompo.domain.model.SlotComida
import com.vic.recompo.domain.model.TipoConversacion
import java.time.Instant
import java.time.LocalDate

class Converters {

    @TypeConverter fun localDateToString(v: LocalDate?): String? = v?.toString()
    @TypeConverter fun stringToLocalDate(v: String?): LocalDate? = v?.let { LocalDate.parse(it) }

    @TypeConverter fun instantToLong(v: Instant?): Long? = v?.toEpochMilli()
    @TypeConverter fun longToInstant(v: Long?): Instant? = v?.let { Instant.ofEpochMilli(it) }

    @TypeConverter fun slotComidaToString(v: SlotComida?): String? = v?.name
    @TypeConverter fun stringToSlotComida(v: String?): SlotComida? = v?.let { SlotComida.valueOf(it) }

    @TypeConverter fun grupoMuscularToString(v: GrupoMuscular?): String? = v?.name
    @TypeConverter fun stringToGrupoMuscular(v: String?): GrupoMuscular? = v?.let { GrupoMuscular.valueOf(it) }

    @TypeConverter fun patronMovimientoToString(v: PatronMovimiento?): String? = v?.name
    @TypeConverter fun stringToPatronMovimiento(v: String?): PatronMovimiento? = v?.let { PatronMovimiento.valueOf(it) }

    @TypeConverter fun estadoSesionToString(v: EstadoSesion?): String? = v?.name
    @TypeConverter fun stringToEstadoSesion(v: String?): EstadoSesion? = v?.let { EstadoSesion.valueOf(it) }

    @TypeConverter fun estadoSerieToString(v: EstadoSerie?): String? = v?.name
    @TypeConverter fun stringToEstadoSerie(v: String?): EstadoSerie? = v?.let { EstadoSerie.valueOf(it) }

    @TypeConverter fun motivoOmisionToString(v: MotivoOmision?): String? = v?.name
    @TypeConverter fun stringToMotivoOmision(v: String?): MotivoOmision? = v?.let { MotivoOmision.valueOf(it) }

    @TypeConverter fun origenSesionToString(v: OrigenSesion?): String? = v?.name
    @TypeConverter fun stringToOrigenSesion(v: String?): OrigenSesion? = v?.let { OrigenSesion.valueOf(it) }

    @TypeConverter fun tipoConversacionToString(v: TipoConversacion?): String? = v?.name
    @TypeConverter fun stringToTipoConversacion(v: String?): TipoConversacion? = v?.let { TipoConversacion.valueOf(it) }

    @TypeConverter fun rolMensajeToString(v: RolMensaje?): String? = v?.name
    @TypeConverter fun stringToRolMensaje(v: String?): RolMensaje? = v?.let { RolMensaje.valueOf(it) }

    @TypeConverter fun funcionIAToString(v: FuncionIA?): String? = v?.name
    @TypeConverter fun stringToFuncionIA(v: String?): FuncionIA? = v?.let { FuncionIA.valueOf(it) }

    @TypeConverter fun proveedorIAToString(v: ProveedorIA?): String? = v?.name
    @TypeConverter fun stringToProveedorIA(v: String?): ProveedorIA? = v?.let { ProveedorIA.valueOf(it) }
}
