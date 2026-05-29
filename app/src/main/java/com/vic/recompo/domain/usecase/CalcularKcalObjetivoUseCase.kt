package com.vic.recompo.domain.usecase

import com.vic.recompo.data.db.entity.Actividad
import com.vic.recompo.ui.home.TipoDia

class CalcularKcalObjetivoUseCase {

    fun calcularObjetivoNutricional(
        tipoDia: TipoDia,
        kcalDescanso: Int,
        kcalMusculacion: Int,
        kcalBici: Int
    ): Int = when (tipoDia) {
        TipoDia.DESCANSO -> kcalDescanso
        TipoDia.MUSCULACION -> kcalMusculacion
        TipoDia.BICI -> kcalBici
    }

    fun calcularGastoReal(metabolismoBasal: Int, actividadesHoy: List<Actividad>): Int =
        metabolismoBasal + actividadesHoy.sumOf { it.kcalQuemadas }
}
