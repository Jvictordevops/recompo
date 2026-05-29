package com.vic.recompo.data.backup

import com.vic.recompo.data.UserSettingsStore
import com.vic.recompo.data.db.RecompoDatabase
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// Genera un .xlsx mínimo sin dependencias externas.
// XLSX = ZIP con XML. Solo texto plano, sin estilos ni fórmulas.
private val FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es"))

class XlsxExporter(
    private val database: RecompoDatabase,
    private val store: UserSettingsStore
) {

    suspend fun generarBytes(): ByteArray {
        val settings = store.settings.first()
        val db = database

        val mediciones = db.medicionDao().getAll().first()
        val entradas = db.entradaComidaDao().getAll()
        val actividades = db.actividadDao().getAll().first()
        val sesiones = db.sesionDao().getAll().first()
        val ejerciciosEnSesion = db.ejercicioEnSesionDao().getAll()
        val series = db.serieDao().getAll()
        val ejercicios = db.ejercicioDao().getAll().first().associateBy { it.id }

        val hojas = mutableListOf<Pair<String, List<List<String>>>>()

        // Resumen
        val resumen = mutableListOf(listOf("Campo", "Valor"))
        if (settings != null) {
            resumen += listOf("Nombre", settings.nombre)
            resumen += listOf("Fase actual", settings.faseActual)
            resumen += listOf("Inicio del plan", settings.fechaInicioPlan.format(FMT))
            resumen += listOf("Peso inicial (kg)", settings.pesoInicialKg.toString())
            resumen += listOf("Peso objetivo (kg)", settings.pesoObjetivoKg.toString())
            resumen += listOf("Kcal base/día", settings.kcalBaseDia.toString())
            resumen += listOf("Proteína objetivo (g)", settings.proteinaObjetivoG.toString())
            resumen += listOf("Total mediciones", mediciones.size.toString())
            resumen += listOf("Total sesiones", sesiones.size.toString())
            resumen += listOf("Total entradas comida", entradas.size.toString())
            resumen += listOf("Total actividades", actividades.size.toString())
        }
        hojas += "Resumen" to resumen

        // Mediciones
        val filaMedHeader = listOf(
            "Fecha", "Peso (kg)", "Cintura (cm)", "Cadera (cm)", "Cuello (cm)",
            "Grasa (%)", "Masa grasa (kg)", "Masa magra (kg)", "IMC", "WHR",
            "Fase", "Hito", "Notas"
        )
        val filasMed = mutableListOf(filaMedHeader)
        mediciones.forEach { m ->
            filasMed += listOf(
                m.fecha.format(FMT), m.pesoKg.toString(),
                m.cinturaCm?.toString() ?: "", m.caderaCm?.toString() ?: "",
                m.cuelloCm?.toString() ?: "", m.grasaPct?.toString() ?: "",
                m.masaGrasaKg?.toString() ?: "", m.masaMagraKg?.toString() ?: "",
                m.imc?.toString() ?: "", m.whr?.toString() ?: "",
                m.faseTexto ?: "", m.hito ?: "", m.notas ?: ""
            )
        }
        hojas += "Mediciones" to filasMed

        // Entrenamiento — una fila por EjercicioEnSesion
        val filasTrn = mutableListOf(listOf(
            "Fecha", "Tipo sesión", "Estado", "Ejercicio",
            "Series obj.", "Reps obj.", "Carga obj. (kg)",
            "Series reales", "Reps prom.", "Carga prom. (kg)", "RIR ej.", "Notas"
        ))
        val seriesPorEjEn = series.groupBy { it.ejercicioEnSesionId }
        val sesionesPorId = sesiones.associateBy { it.id }
        ejerciciosEnSesion.forEach { ejEn ->
            val sesion = sesionesPorId[ejEn.sesionId] ?: return@forEach
            val nombreEj = ejercicios[ejEn.ejercicioId]?.nombre ?: ejEn.ejercicioId.toString()
            val seriesEj = seriesPorEjEn[ejEn.id] ?: emptyList()
            val completadas = seriesEj.filter { it.completada }
            val repsProm = if (completadas.isEmpty()) "" else
                String.format(Locale.US, "%.1f", completadas.map { it.repsReales }.average())
            val cargaProm = if (completadas.isEmpty()) "" else
                String.format(Locale.US, "%.1f", completadas.map { it.cargaKg }.average())
            filasTrn += listOf(
                sesion.fechaPrevista.format(FMT), sesion.tipo.name, sesion.estado.name,
                nombreEj, ejEn.seriesObjetivo.toString(),
                "${ejEn.repsObjetivoMin}-${ejEn.repsObjetivoMax}",
                ejEn.cargaObjetivoKg?.toString() ?: "",
                completadas.size.toString(), repsProm, cargaProm,
                ejEn.rir?.toString() ?: "", ejEn.notas ?: ""
            )
        }
        hojas += "Entrenamiento" to filasTrn

        // Nutrición
        val filasNut = mutableListOf(listOf("Fecha", "Slot", "Descripción", "Kcal", "Proteína (g)", "Grasa (g)", "Carbo (g)"))
        entradas.forEach { e ->
            filasNut += listOf(
                e.fecha.format(FMT), e.slot.name, e.textoLibre,
                e.kcal.toString(), e.proteinaG.toString(), e.grasaG.toString(), e.carboG.toString()
            )
        }
        hojas += "Nutrición" to filasNut

        // Actividad
        val filasAct = mutableListOf(listOf("Fecha", "Tipo", "Descripción", "Duración (min)", "Kcal quemadas"))
        actividades.forEach { a ->
            filasAct += listOf(
                a.fecha.format(FMT), a.tipo, a.descripcion ?: "",
                a.duracionMin?.toString() ?: "", a.kcalQuemadas.toString()
            )
        }
        hojas += "Actividad" to filasAct

        val bytes = buildXlsx(hojas)
        Timber.d("XLSX generado: ${bytes.size} bytes, ${hojas.size} hojas")
        return bytes
    }

    private fun buildXlsx(hojas: List<Pair<String, List<List<String>>>>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->

            // [Content_Types].xml
            zip.entry("[Content_Types].xml") {
                append("""<?xml version="1.0" encoding="UTF-8"?>""")
                append("""<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">""")
                append("""<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>""")
                append("""<Default Extension="xml" ContentType="application/xml"/>""")
                append("""<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""")
                hojas.forEachIndexed { i, _ ->
                    append("""<Override PartName="/xl/worksheets/sheet${i + 1}.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""")
                }
                append("</Types>")
            }

            // _rels/.rels
            zip.entry("_rels/.rels") {
                append("""<?xml version="1.0" encoding="UTF-8"?>""")
                append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
                append("""<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>""")
                append("</Relationships>")
            }

            // xl/_rels/workbook.xml.rels
            zip.entry("xl/_rels/workbook.xml.rels") {
                append("""<?xml version="1.0" encoding="UTF-8"?>""")
                append("""<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""")
                hojas.forEachIndexed { i, _ ->
                    append("""<Relationship Id="rId${i + 1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${i + 1}.xml"/>""")
                }
                append("</Relationships>")
            }

            // xl/workbook.xml
            zip.entry("xl/workbook.xml") {
                append("""<?xml version="1.0" encoding="UTF-8"?>""")
                append("""<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">""")
                append("<sheets>")
                hojas.forEachIndexed { i, (nombre, _) ->
                    append("""<sheet name="${nombre.xmlEscape()}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""")
                }
                append("</sheets></workbook>")
            }

            // xl/worksheets/sheetN.xml
            hojas.forEachIndexed { i, (_, filas) ->
                zip.entry("xl/worksheets/sheet${i + 1}.xml") {
                    append("""<?xml version="1.0" encoding="UTF-8"?>""")
                    append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>""")
                    filas.forEachIndexed { r, cols ->
                        append("""<row r="${r + 1}">""")
                        cols.forEachIndexed { c, v ->
                            val col = colLetter(c)
                            append("""<c r="$col${r + 1}" t="inlineStr"><is><t>${v.xmlEscape()}</t></is></c>""")
                        }
                        append("</row>")
                    }
                    append("</sheetData></worksheet>")
                }
            }
        }
        return out.toByteArray()
    }

    private fun ZipOutputStream.entry(name: String, block: StringBuilder.() -> Unit) {
        putNextEntry(ZipEntry(name))
        val sb = StringBuilder()
        sb.block()
        write(sb.toString().toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun colLetter(col: Int): String {
        var c = col
        val sb = StringBuilder()
        do {
            sb.insert(0, ('A' + c % 26))
            c = c / 26 - 1
        } while (c >= 0)
        return sb.toString()
    }

    private fun String.xmlEscape(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}
