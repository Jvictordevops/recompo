package com.vic.recompo.domain.calc

import com.vic.recompo.domain.model.Sexo
import kotlin.math.log10

// Hodgdon-Beckett Navy formula (cm). Fórmula lineal con coeficientes distintos para cm/pulgadas.
fun calcularGrasaNavy(
    sexo: Sexo,
    alturaCm: Int,
    cinturaCm: Double?,
    cuelloCm: Double?,
    caderaCm: Double?
): Double? {
    val cintura = cinturaCm ?: return null
    val cuello = cuelloCm ?: return null
    val altura = alturaCm.toDouble()
    return when (sexo) {
        Sexo.HOMBRE -> {
            val diff = cintura - cuello
            if (diff <= 0) return null
            val density = 1.0324 - 0.19077 * log10(diff) + 0.15456 * log10(altura)
            if (density <= 0) return null
            495.0 / density - 450.0
        }
        Sexo.MUJER -> {
            val cadera = caderaCm ?: return null
            val sum = cintura + cadera - cuello
            if (sum <= 0) return null
            val density = 1.29579 - 0.35004 * log10(sum) + 0.22100 * log10(altura)
            if (density <= 0) return null
            495.0 / density - 450.0
        }
    }
}

fun calcularImc(pesoKg: Double, alturaCm: Int): Double {
    val alturaM = alturaCm / 100.0
    return pesoKg / (alturaM * alturaM)
}

fun calcularMasaGrasaKg(pesoKg: Double, grasaPct: Double): Double =
    pesoKg * grasaPct / 100.0

fun calcularMasaMagraKg(pesoKg: Double, masaGrasaKg: Double): Double =
    pesoKg - masaGrasaKg

fun calcularWhr(cinturaCm: Double?, caderaCm: Double?): Double? {
    if (cinturaCm == null || caderaCm == null || caderaCm <= 0) return null
    return cinturaCm / caderaCm
}
