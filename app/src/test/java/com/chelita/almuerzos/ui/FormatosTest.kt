package com.chelita.almuerzos.ui

import com.chelita.almuerzos.datos.LineaPedidoEntity
import com.chelita.almuerzos.datos.PedidoConLineas
import com.chelita.almuerzos.datos.PedidoEntity
import com.chelita.almuerzos.dominio.EstadoPedido
import com.chelita.almuerzos.dominio.Producto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class FormatosTest {

    // María: 2 almuerzos y 1 sopa para llevar = $5.50 + tarrinas $0.95 = $6.45.
    // Las líneas vienen desordenadas a propósito, como podrían venir de la base de datos.
    private val pedido = PedidoConLineas(
        pedido = PedidoEntity(
            id = 1,
            nombre = "María",
            paraLlevar = true,
            observaciones = "",
            estado = EstadoPedido.PENDIENTE,
            fecha = LocalDate.of(2026, 11, 2),
            creadoEn = 0,
            tarrinasSegundo = 2,
            tarrinasSopa = 3,
            envasesProteina = 0,
            costoTarrinas = 95,
            total = 645,
        ),
        lineas = listOf(
            LineaPedidoEntity(pedidoId = 1, producto = Producto.SOPA, cantidad = 1, precioUnitario = 50),
            LineaPedidoEntity(pedidoId = 1, producto = Producto.ALMUERZO, cantidad = 2, precioUnitario = 250),
        ),
    )

    @Test
    fun `resume los productos en el orden del menu`() {
        assertEquals("2 Almuerzo · 1 Sopa", resumenProductos(pedido))
    }

    @Test
    fun `detalla solo las tarrinas que se usan`() {
        assertEquals("Tarrinas (2 segundo, 3 sopa)", textoTarrinas(2, 3, 0))
        assertEquals("Tarrinas (1 proteína)", textoTarrinas(0, 0, 1))
    }

    @Test
    fun `arma la cuenta para WhatsApp`() {
        val esperado = """
            *Almuerzos Chelita*
            Pedido de María (para llevar)
            2 Almuerzo: $5.00
            1 Sopa: $0.50
            Tarrinas (2 segundo, 3 sopa): $0.95
            *Total: $6.45*
        """.trimIndent()

        assertEquals(esperado, textoCobro(pedido))
    }

    @Test
    fun `la cuenta de un pedido para comer aqui no lleva tarrinas`() {
        val aqui = pedido.copy(pedido = pedido.pedido.copy(paraLlevar = false, costoTarrinas = 0, total = 550))

        assertEquals(
            "*Almuerzos Chelita*\nPedido de María\n2 Almuerzo: $5.00\n1 Sopa: $0.50\n*Total: $5.50*",
            textoCobro(aqui),
        )
    }

    @Test
    fun `escribe las fechas en español`() {
        val fecha = LocalDate.of(2026, 11, 1)
        assertEquals("Domingo 1 de noviembre", fechaLarga(fecha))
        assertEquals("Domingo 1", diaDelMes(fecha))
        assertEquals("Noviembre 2026", mesYAnio(fecha))
    }

    @Test
    fun `muestra la hora del pedido`() {
        val mediodia = LocalDateTime.of(2026, 11, 2, 12, 5).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals("12:05", horaDe(mediodia, ZoneOffset.UTC))
    }
}
