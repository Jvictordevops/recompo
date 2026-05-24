package com.vic.recompo.domain.usecase

import com.vic.recompo.data.db.entity.Actividad

class CalcularKcalObjetivoUseCase {
    fun calcular(kcalBaseDia: Int, actividadesHoy: List<Actividad>): Int =
        kcalBaseDia + actividadesHoy.sumOf { it.kcalQuemadas }
}
