package com.chelita.almuerzos.ui.pedidos

import com.chelita.almuerzos.datos.LineaPedidoEntity
import com.chelita.almuerzos.datos.PedidoConLineas
import com.chelita.almuerzos.datos.PedidoEntity
import com.chelita.almuerzos.dominio.EstadoPedido
import com.chelita.almuerzos.dominio.Precios
import com.chelita.almuerzos.dominio.Producto.ALMUERZO
import com.chelita.almuerzos.dominio.Producto.SOPA
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FormularioPedidoTest {

    @Test
    fun `sumar y restar cambian la cantidad sin bajar de cero`() {
        var f = FormularioPedido()
        f = f.sumar(ALMUERZO).sumar(ALMUERZO).sumar(SOPA)
        assertEquals(2, f.cantidad(ALMUERZO))
        assertEquals(1, f.cantidad(SOPA))

        f = f.restar(SOPA).restar(SOPA).restar(SOPA)
        assertEquals(0, f.cantidad(SOPA))
    }

    @Test
    fun `solo se puede guardar con nombre y al menos un producto`() {
        assertFalse(FormularioPedido().sePuedeGuardar)
        assertFalse(FormularioPedido(nombre = "Ana").sePuedeGuardar)
        assertFalse(FormularioPedido(nombre = "  ").sumar(ALMUERZO).sePuedeGuardar)
        assertFalse(FormularioPedido(nombre = "Ana").sumar(ALMUERZO).restar(ALMUERZO).sePuedeGuardar)
        assertTrue(FormularioPedido(nombre = "Ana").sumar(ALMUERZO).sePuedeGuardar)
    }

    @Test
    fun `el total se calcula en vivo segun aqui o para llevar`() {
        val f = FormularioPedido(nombre = "Ana").sumar(ALMUERZO)
        assertEquals(250, f.cobro(Precios()).total)
        assertEquals(290, f.copy(paraLlevar = true).cobro(Precios()).total)
    }

    @Test
    fun `se llena desde un pedido guardado`() {
        val guardado = PedidoConLineas(
            pedido = PedidoEntity(
                id = 7, nombre = "Rosa", paraLlevar = true, observaciones = "sin ají",
                estado = EstadoPedido.SERVIDO, fecha = LocalDate.of(2026, 11, 2), creadoEn = 0,
                tarrinasSegundo = 1, tarrinasSopa = 1, envasesProteina = 0, costoTarrinas = 40, total = 290,
            ),
            lineas = listOf(LineaPedidoEntity(7, ALMUERZO, 1, 250)),
        )
        val f = FormularioPedido.desde(guardado)
        assertEquals(FormularioPedido("Rosa", true, mapOf(ALMUERZO to 1), "sin ají", EstadoPedido.SERVIDO), f)
    }

    @Test
    fun `el estado avanza de pendiente a servido a pagado`() {
        assertEquals(EstadoPedido.SERVIDO, EstadoPedido.PENDIENTE.siguiente)
        assertEquals(EstadoPedido.PAGADO, EstadoPedido.SERVIDO.siguiente)
        assertEquals(null, EstadoPedido.PAGADO.siguiente)
        assertEquals("Entregado", EstadoPedido.SERVIDO.nombrePara(paraLlevar = true))
        assertEquals("Servido", EstadoPedido.SERVIDO.nombrePara(paraLlevar = false))
    }
}
