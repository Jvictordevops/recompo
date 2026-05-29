package com.vic.recompo.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DeloadCalendarTest {

    @Test
    fun isDeloadWeek_returns_true_for_first_day_semana11() {
        assertTrue(DeloadCalendar.isDeloadWeek(LocalDate.of(2026, 7, 13)))
    }

    @Test
    fun isDeloadWeek_returns_true_for_last_day_semana11() {
        assertTrue(DeloadCalendar.isDeloadWeek(LocalDate.of(2026, 7, 19)))
    }

    @Test
    fun isDeloadWeek_returns_false_for_day_before_semana11() {
        assertFalse(DeloadCalendar.isDeloadWeek(LocalDate.of(2026, 7, 12)))
    }

    @Test
    fun isDeloadWeek_returns_false_for_day_after_semana11() {
        assertFalse(DeloadCalendar.isDeloadWeek(LocalDate.of(2026, 7, 20)))
    }

    @Test
    fun isDeloadWeek_returns_false_for_today_before_any_deload() {
        assertFalse(DeloadCalendar.isDeloadWeek(LocalDate.of(2026, 5, 29)))
    }

    @Test
    fun isDeloadWeek_returns_true_for_semana17() {
        assertTrue(DeloadCalendar.isDeloadWeek(LocalDate.of(2026, 8, 27)))
    }

    @Test
    fun isDeloadWeek_returns_true_for_semana23() {
        assertTrue(DeloadCalendar.isDeloadWeek(LocalDate.of(2026, 10, 5)))
    }

    @Test
    fun daysToNextDeload_returns_correct_days_before_semana11() {
        // 2026-06-01 → semana 11 starts 2026-07-13 = 42 days
        assertEquals(42, DeloadCalendar.daysToNextDeload(LocalDate.of(2026, 6, 1)))
    }

    @Test
    fun daysToNextDeload_skips_to_semana17_when_past_semana11() {
        // Day after semana 11 ends → next is semana 17
        val dayAfterSemana11 = LocalDate.of(2026, 7, 20)
        val days = DeloadCalendar.daysToNextDeload(dayAfterSemana11)
        assertEquals(35, days) // 2026-07-20 → 2026-08-24 = 35 days
    }

    @Test
    fun daysToNextDeload_returns_null_after_last_deload() {
        assertNull(DeloadCalendar.daysToNextDeload(LocalDate.of(2026, 10, 12)))
    }

    @Test
    fun deloadContextText_active_during_deload_week() {
        val text = DeloadCalendar.deloadContextText(LocalDate.of(2026, 7, 15))
        assertTrue(text.contains("DELOAD ACTIVA"))
    }

    @Test
    fun deloadContextText_conservative_warning_within_7_days() {
        // 2026-07-06 → semana 11 starts 2026-07-13 = 7 days
        val text = DeloadCalendar.deloadContextText(LocalDate.of(2026, 7, 6))
        assertTrue(text.contains("conservador"))
    }

    @Test
    fun deloadContextText_null_after_all_deloads() {
        val text = DeloadCalendar.deloadContextText(LocalDate.of(2026, 11, 1))
        assertTrue(text.contains("Sin más semanas"))
    }
}
