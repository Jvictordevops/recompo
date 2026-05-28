package com.vic.recompo.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UserSettingsValidationTest {

    @Test
    fun altura_valida_en_rango() {
        assertEquals(180, UserSettingsValidation.parseAltura("180").getOrNull())
        assertEquals(100, UserSettingsValidation.parseAltura("100").getOrNull())
        assertEquals(250, UserSettingsValidation.parseAltura("250").getOrNull())
    }

    @Test
    fun altura_fuera_de_rango_falla() {
        assertTrue(UserSettingsValidation.parseAltura("99").isFailure)
        assertTrue(UserSettingsValidation.parseAltura("251").isFailure)
        assertTrue(UserSettingsValidation.parseAltura("abc").isFailure)
        assertTrue(UserSettingsValidation.parseAltura("").isFailure)
    }

    @Test
    fun peso_positivo_acepta_punto_y_coma() {
        assertEquals(82.5, UserSettingsValidation.parsePesoKg("82.5").getOrNull()!!, 0.0001)
        assertEquals(82.5, UserSettingsValidation.parsePesoKg("82,5").getOrNull()!!, 0.0001)
    }

    @Test
    fun peso_cero_o_negativo_falla() {
        assertTrue(UserSettingsValidation.parsePesoKg("0").isFailure)
        assertTrue(UserSettingsValidation.parsePesoKg("-1.5").isFailure)
        assertTrue(UserSettingsValidation.parsePesoKg("").isFailure)
    }

    @Test
    fun kcal_positivo_ok() {
        assertEquals(1900, UserSettingsValidation.parseKcal("1900").getOrNull())
    }

    @Test
    fun kcal_cero_o_negativo_falla() {
        assertTrue(UserSettingsValidation.parseKcal("0").isFailure)
        assertTrue(UserSettingsValidation.parseKcal("-100").isFailure)
        assertTrue(UserSettingsValidation.parseKcal("abc").isFailure)
    }

    @Test
    fun proteina_positiva_ok() {
        assertEquals(160, UserSettingsValidation.parseProteinaG("160").getOrNull())
        assertTrue(UserSettingsValidation.parseProteinaG("0").isFailure)
    }

    @Test
    fun fecha_iso_valida() {
        val res = UserSettingsValidation.parseLocalDate("2026-05-27", UserSettingsValidation.ERROR_FECHA_INICIO_PLAN)
        assertEquals(LocalDate.of(2026, 5, 27), res.getOrNull())
    }

    @Test
    fun fecha_invalida_devuelve_failure_con_mensaje() {
        val res = UserSettingsValidation.parseLocalDate("27/05/2026", UserSettingsValidation.ERROR_FECHA_NACIMIENTO)
        assertTrue(res.isFailure)
        assertEquals(UserSettingsValidation.ERROR_FECHA_NACIMIENTO, res.exceptionOrNull()?.message)
    }

    @Test
    fun nombre_y_fase_validan_blanco() {
        assertTrue(UserSettingsValidation.parseNombre("  ").isFailure)
        assertEquals("Vic", UserSettingsValidation.parseNombre("  Vic  ").getOrNull())
        assertTrue(UserSettingsValidation.parseFase("").isFailure)
        assertEquals("Fase 1", UserSettingsValidation.parseFase("Fase 1").getOrNull())
    }
}
