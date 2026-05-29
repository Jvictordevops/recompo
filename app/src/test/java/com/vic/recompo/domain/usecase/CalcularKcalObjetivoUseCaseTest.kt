package com.vic.recompo.domain.usecase

import com.vic.recompo.data.db.entity.Actividad
import com.vic.recompo.ui.home.TipoDia
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

    // --- calcularObjetivoNutricional ---

    @Test
    fun `objetivo descanso devuelve kcalDescanso`() {
        assertEquals(1900, useCase.calcularObjetivoNutricional(TipoDia.DESCANSO, 1900, 2050, 2150))
    }

    @Test
    fun `objetivo musculacion devuelve kcalMusculacion`() {
        assertEquals(2050, useCase.calcularObjetivoNutricional(TipoDia.MUSCULACION, 1900, 2050, 2150))
    }

    @Test
    fun `objetivo bici devuelve kcalBici`() {
        assertEquals(2150, useCase.calcularObjetivoNutricional(TipoDia.BICI, 1900, 2050, 2150))
    }

    // --- calcularGastoReal ---

    @Test
    fun `gasto real sin actividades es el metabolismo basal`() {
        assertEquals(1830, useCase.calcularGastoReal(1830, emptyList()))
    }

    @Test
    fun `gasto real suma una actividad`() {
        assertEquals(2230, useCase.calcularGastoReal(1830, listOf(actividad(400))))
    }

    @Test
    fun `gasto real suma varias actividades`() {
        assertEquals(2430, useCase.calcularGastoReal(1830, listOf(actividad(400), actividad(200))))
    }
}
