package com.vic.recompo.domain

import com.vic.recompo.domain.calc.calcularGrasaNavy
import com.vic.recompo.domain.calc.calcularImc
import com.vic.recompo.domain.calc.calcularMasaGrasaKg
import com.vic.recompo.domain.calc.calcularMasaMagraKg
import com.vic.recompo.domain.calc.calcularWhr
import com.vic.recompo.domain.model.Sexo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MedicionCalcsTest {

    @Test
    fun calcula_grasa_navy_hombre_con_mediciones_validas() {
        // cintura=87, cuello=38, altura=180 → Hodgdon-Beckett ~17.7%
        val result = calcularGrasaNavy(Sexo.HOMBRE, 180, 87.0, 38.0, null)
        assertNotNull(result)
        assertEquals(17.7, result!!, 0.5)
    }

    @Test
    fun calcula_grasa_navy_mujer_con_mediciones_validas() {
        // cintura=75, cadera=95, cuello=33, altura=165 → Hodgdon-Beckett ~27%
        val result = calcularGrasaNavy(Sexo.MUJER, 165, 75.0, 33.0, 95.0)
        assertNotNull(result)
        assertEquals(27.0, result!!, 1.0)
    }

    @Test
    fun grasa_navy_hombre_devuelve_null_si_falta_cintura() {
        assertNull(calcularGrasaNavy(Sexo.HOMBRE, 180, null, 38.0, null))
    }

    @Test
    fun grasa_navy_hombre_devuelve_null_si_falta_cuello() {
        assertNull(calcularGrasaNavy(Sexo.HOMBRE, 180, 87.0, null, null))
    }

    @Test
    fun grasa_navy_mujer_devuelve_null_si_falta_cadera() {
        assertNull(calcularGrasaNavy(Sexo.MUJER, 165, 75.0, 33.0, null))
    }

    @Test
    fun grasa_navy_hombre_devuelve_null_si_cintura_menor_que_cuello() {
        assertNull(calcularGrasaNavy(Sexo.HOMBRE, 180, 30.0, 40.0, null))
    }

    @Test
    fun calcula_imc_correctamente() {
        // 91kg / 1.80m^2 ≈ 28.09
        assertEquals(28.09, calcularImc(91.0, 180), 0.1)
    }

    @Test
    fun calcula_masa_grasa_kg_correctamente() {
        assertEquals(21.84, calcularMasaGrasaKg(91.0, 24.0), 0.01)
    }

    @Test
    fun calcula_masa_magra_kg_correctamente() {
        assertEquals(69.16, calcularMasaMagraKg(91.0, 21.84), 0.01)
    }

    @Test
    fun calcula_whr_correctamente() {
        // 87 / 95 ≈ 0.916
        assertEquals(0.916, calcularWhr(87.0, 95.0)!!, 0.001)
    }

    @Test
    fun whr_devuelve_null_si_falta_cintura() {
        assertNull(calcularWhr(null, 95.0))
    }

    @Test
    fun whr_devuelve_null_si_falta_cadera() {
        assertNull(calcularWhr(87.0, null))
    }
}
