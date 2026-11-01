package com.chelita.almuerzos.dominio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DineroTest {

    @Test
    fun `entiende los montos como los escribe la mesera`() {
        assertEquals(1000, "10".aCentavos())
        assertEquals(750, "7.5".aCentavos())
        assertEquals(735, "7.35".aCentavos())
        assertEquals(735, "7,35".aCentavos())
        assertEquals(800, "$8".aCentavos())
        assertEquals(500, " 5 ".aCentavos())
        // Mientras escribe "7.50" pasa por "7.", que ya cuenta como $7.00.
        assertEquals(700, "7.".aCentavos())
        assertEquals(0, "0".aCentavos())
    }

    @Test
    fun `rechaza lo que no es un monto`() {
        assertNull("".aCentavos())
        assertNull("abc".aCentavos())
        assertNull("7.355".aCentavos())
        assertNull("-5".aCentavos())
        assertNull("1.2.3".aCentavos())
        assertNull(".50".aCentavos())
    }

    @Test
    fun `sugiere el siguiente dolar y los billetes que alcanzan`() {
        assertEquals(listOf(800, 1000, 2000), montosSugeridos(735))
        assertEquals(listOf(300, 500, 1000, 2000), montosSugeridos(250))
        // Si el total ya es un dólar exacto, no se repite como sugerencia.
        assertEquals(listOf(500, 1000, 2000), montosSugeridos(300))
        // Totales grandes: se redondea a los siguientes billetes.
        assertEquals(listOf(2600, 3000, 4000), montosSugeridos(2575))
        assertEquals(emptyList<Int>(), montosSugeridos(0))
    }
}
