package com.chelita.almuerzos.dominio

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class PeriodoTest {

    private val hoy = LocalDate.of(2026, 11, 18)

    @Test
    fun `hoy es solo el dia de hoy`() {
        assertEquals(Rango(hoy, hoy), Periodo.HOY.rango(hoy))
    }

    @Test
    fun `este mes va desde el dia 1 hasta hoy`() {
        assertEquals(Rango(LocalDate.of(2026, 11, 1), hoy), Periodo.ESTE_MES.rango(hoy))
    }

    @Test
    fun `el mes anterior es el mes completo`() {
        assertEquals(
            Rango(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)),
            Periodo.MES_ANTERIOR.rango(hoy),
        )
    }

    @Test
    fun `en enero el mes anterior es diciembre del año pasado`() {
        assertEquals(
            Rango(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 31)),
            Periodo.MES_ANTERIOR.rango(LocalDate.of(2027, 1, 5)),
        )
    }

    @Test
    fun `respeta los febreros de año bisiesto`() {
        assertEquals(
            Rango(LocalDate.of(2028, 2, 1), LocalDate.of(2028, 2, 29)),
            Periodo.MES_ANTERIOR.rango(LocalDate.of(2028, 3, 31)),
        )
    }
}
