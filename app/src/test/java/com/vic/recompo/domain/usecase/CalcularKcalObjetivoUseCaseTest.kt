package com.vic.recompo.domain.usecase

import com.vic.recompo.data.db.entity.Actividad
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CalcularKcalObjetivoUseCaseTest {

    private val useCase = CalcularKcalObjetivoUseCase()
    private val hoy = LocalDate.of(2026, 5, 24)

    private fun actividad(kcal: Int) = Actividad(
        fecha = hoy,
        tipo = "bici",
        descripcion = null,
        duracionMin = null,
        kcalQuemadas = kcal
    )

    @Test
    fun `sin actividades devuelve kcalBaseDia`() {
        assertEquals(1900, useCase.calcular(1900, emptyList()))
    }

    @Test
    fun `una actividad suma sus kcal`() {
        assertEquals(2300, useCase.calcular(1900, listOf(actividad(400))))
    }

    @Test
    fun `varias actividades suman todas`() {
        assertEquals(2500, useCase.calcular(1900, listOf(actividad(400), actividad(200))))
    }

    @Test
    fun `base cero con actividad devuelve solo las kcal de la actividad`() {
        assertEquals(300, useCase.calcular(0, listOf(actividad(300))))
    }

    @Test
    fun `base y actividades cero devuelve cero`() {
        assertEquals(0, useCase.calcular(0, emptyList()))
    }
}
