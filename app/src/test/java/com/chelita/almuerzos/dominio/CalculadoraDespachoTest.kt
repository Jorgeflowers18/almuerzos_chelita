package com.chelita.almuerzos.dominio

import com.chelita.almuerzos.dominio.Producto.ALMUERZO
import com.chelita.almuerzos.dominio.Producto.ALMUERZO_MIXTO
import com.chelita.almuerzos.dominio.Producto.PORCION_PROTEINA
import com.chelita.almuerzos.dominio.Producto.SEGUNDO
import com.chelita.almuerzos.dominio.Producto.SEGUNDO_MIXTO
import com.chelita.almuerzos.dominio.Producto.SOPA
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculadoraDespachoTest {

    @Test
    fun `sin pedidos no hay nada por servir`() {
        assertEquals(PorServir(), CalculadoraDespacho.porServir(emptyList()))
    }

    @Test
    fun `suma segundos, sopas y bebidas de todos los pedidos`() {
        val pedidos = listOf(
            mapOf(ALMUERZO to 2, SOPA to 1), // 2 segundos, 3 sopas, 2 bebidas
            mapOf(SEGUNDO to 1), // 1 segundo, 1 bebida
            mapOf(SOPA to 2), // 2 sopas, sin bebida
        )

        assertEquals(
            PorServir(segundos = 3, mixtos = 0, sopas = 5, porciones = 0, bebidas = 3),
            CalculadoraDespacho.porServir(pedidos),
        )
    }

    @Test
    fun `los mixtos cuentan como segundos y se indican aparte`() {
        val pedidos = listOf(
            mapOf(ALMUERZO_MIXTO to 1), // 1 segundo mixto, 1 sopa, 1 bebida
            mapOf(SEGUNDO_MIXTO to 2, SEGUNDO to 1), // 3 segundos (2 mixtos), 3 bebidas
        )

        assertEquals(
            PorServir(segundos = 4, mixtos = 3, sopas = 1, porciones = 0, bebidas = 4),
            CalculadoraDespacho.porServir(pedidos),
        )
    }

    @Test
    fun `las porciones aparte no son segundos ni llevan bebida`() {
        val pedidos = listOf(mapOf(PORCION_PROTEINA to 4), mapOf(PORCION_PROTEINA to 1, SOPA to 1))

        assertEquals(
            PorServir(segundos = 0, mixtos = 0, sopas = 1, porciones = 5, bebidas = 0),
            CalculadoraDespacho.porServir(pedidos),
        )
    }
}
