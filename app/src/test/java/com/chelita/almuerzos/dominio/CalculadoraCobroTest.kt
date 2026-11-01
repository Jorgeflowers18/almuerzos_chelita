package com.chelita.almuerzos.dominio

import com.chelita.almuerzos.dominio.Producto.ALMUERZO
import com.chelita.almuerzos.dominio.Producto.ALMUERZO_MIXTO
import com.chelita.almuerzos.dominio.Producto.PORCION_PROTEINA
import com.chelita.almuerzos.dominio.Producto.SEGUNDO
import com.chelita.almuerzos.dominio.Producto.SEGUNDO_MIXTO
import com.chelita.almuerzos.dominio.Producto.SOPA
import org.junit.Assert.assertEquals
import org.junit.Test

// Pruebas automáticas: cada función marcada con @Test arma un pedido y compara
// el resultado con el valor que debería dar. Si alguno no coincide, la prueba falla.
class CalculadoraCobroTest {

    private val precios = Precios()

    // Atajo para no repetir CalculadoraCobro.calcular(..., precios) en cada prueba.
    private fun total(paraLlevar: Boolean, vararg items: Pair<Producto, Int>): Int =
        CalculadoraCobro.calcular(mapOf(*items), paraLlevar, precios).total

    // --- Precios en el local (sin tarrinas) ---

    @Test
    fun `cada producto cuesta su precio en el local`() {
        assertEquals(250, total(false, ALMUERZO to 1))
        assertEquals(325, total(false, ALMUERZO_MIXTO to 1))
        assertEquals(200, total(false, SEGUNDO to 1))
        assertEquals(275, total(false, SEGUNDO_MIXTO to 1))
        assertEquals(50, total(false, SOPA to 1))
        assertEquals(75, total(false, PORCION_PROTEINA to 1))
    }

    @Test
    fun `pedido vacio cuesta cero`() {
        assertEquals(0, total(false))
        assertEquals(0, total(true))
    }

    // --- Para llevar (con tarrinas) ---

    @Test
    fun `almuerzo para llevar suma tarrina de segundo y de sopa`() {
        assertEquals(250 + 25 + 15, total(true, ALMUERZO to 1))
        assertEquals(325 + 25 + 15, total(true, ALMUERZO_MIXTO to 1))
    }

    @Test
    fun `segundo para llevar suma solo tarrina de segundo`() {
        assertEquals(200 + 25, total(true, SEGUNDO to 1))
        assertEquals(275 + 25, total(true, SEGUNDO_MIXTO to 1))
    }

    @Test
    fun `sopa para llevar suma tarrina pequena`() {
        assertEquals(50 + 15, total(true, SOPA to 1))
    }

    @Test
    fun `hasta 3 porciones de proteina van en un solo envase`() {
        assertEquals(75 + 15, total(true, PORCION_PROTEINA to 1))
        assertEquals(3 * 75 + 15, total(true, PORCION_PROTEINA to 3))
        assertEquals(4 * 75 + 2 * 15, total(true, PORCION_PROTEINA to 4))
        assertEquals(6 * 75 + 2 * 15, total(true, PORCION_PROTEINA to 6))
        assertEquals(7 * 75 + 3 * 15, total(true, PORCION_PROTEINA to 7))
    }

    @Test
    fun `pedido combinado para llevar cuenta cada tarrina`() {
        // 2 almuerzos + 1 sopa adicional + 1 segundo mixto + 2 porciones
        val cobro = CalculadoraCobro.calcular(
            mapOf(ALMUERZO to 2, SOPA to 1, SEGUNDO_MIXTO to 1, PORCION_PROTEINA to 2),
            paraLlevar = true,
            precios = precios,
        )
        assertEquals(3, cobro.tarrinasSegundo)   // 2 almuerzos + 1 segundo mixto
        assertEquals(3, cobro.tarrinasSopa)      // 2 almuerzos + 1 sopa
        assertEquals(1, cobro.envasesProteina)   // 2 porciones caben en 1 envase
        assertEquals(500 + 50 + 275 + 150, cobro.subtotalPlatos)
        assertEquals(3 * 25 + 3 * 15 + 15, cobro.costoTarrinas)
        assertEquals(975 + 135, cobro.total)
    }

    @Test
    fun `en el local no se cobran tarrinas`() {
        val cobro = CalculadoraCobro.calcular(mapOf(ALMUERZO to 2, PORCION_PROTEINA to 1), false, precios)
        assertEquals(0, cobro.costoTarrinas)
        assertEquals(0, cobro.tarrinasSegundo)
        assertEquals(575, cobro.total)
    }

    // --- Desglose ---

    @Test
    fun `el desglose omite productos en cero y respeta el orden del menu`() {
        val cobro = CalculadoraCobro.calcular(mapOf(SOPA to 1, ALMUERZO to 2, SEGUNDO to 0), false, precios)
        assertEquals(listOf(ALMUERZO, SOPA), cobro.lineas.map { it.producto })
        assertEquals(500, cobro.lineas[0].subtotal)
    }

    // --- Precios editados ---

    @Test
    fun `si cambian los precios el calculo usa los nuevos`() {
        val nuevos = Precios(almuerzo = 300, proteinaExtra = 100, tarrinaSegundo = 30)
        val cobro = CalculadoraCobro.calcular(mapOf(ALMUERZO_MIXTO to 1), true, nuevos)
        assertEquals(300 + 100 + 30 + 15, cobro.total)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `no se aceptan cantidades negativas`() {
        total(false, ALMUERZO to -1)
    }

    // --- Formato de dinero ---

    @Test
    fun `formato de dinero`() {
        assertEquals("$2.50", 250.comoDinero())
        assertEquals("$0.15", 15.comoDinero())
        assertEquals("$0.05", 5.comoDinero())
        assertEquals("$0.00", 0.comoDinero())
        assertEquals("$11.10", 1110.comoDinero())
    }
}
