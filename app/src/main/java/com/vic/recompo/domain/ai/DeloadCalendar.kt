package com.vic.recompo.domain.ai

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object DeloadCalendar {

    // Semanas de deload del plan v2 (fechas fijas del calendario de Vic)
    // DT: mover a Settings si en el futuro se necesita edición manual
    private data class DeloadWeek(val start: LocalDate, val end: LocalDate) {
        operator fun contains(date: LocalDate) = date in start..end
    }

    private val SEMANAS = listOf(
        DeloadWeek(LocalDate.of(2026, 7, 13), LocalDate.of(2026, 7, 19)),  // semana 11
        DeloadWeek(LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 30)),  // semana 17
        DeloadWeek(LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 11)) // semana 23
    )

    fun isDeloadWeek(date: LocalDate): Boolean = SEMANAS.any { date in it }

    fun daysToNextDeload(date: LocalDate): Int? {
        val nextStart = SEMANAS.map { it.start }.filter { it > date }.minOrNull()
        return nextStart?.let { ChronoUnit.DAYS.between(date, it).toInt() }
    }

    fun deloadContextText(date: LocalDate): String {
        if (isDeloadWeek(date)) return "SEMANA DE DELOAD ACTIVA. Propón misma carga al 60% del volumen de series."
        val days = daysToNextDeload(date) ?: return "Sin más semanas de deload programadas en el plan actual."
        val nextStart = SEMANAS.map { it.start }.filter { it > date }.minOrNull()!!
        return when {
            days <= 7 -> "Próximo deload en $days días ($nextStart). Sé conservador: no fuerces progresión esta semana."
            days <= 14 -> "Próximo deload en $days días ($nextStart)."
            else -> "Siguiente deload: $nextStart (en $days días)."
        }
    }
}
