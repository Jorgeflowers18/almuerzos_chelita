package com.chelita.almuerzos.ui.resumen

import com.chelita.almuerzos.datos.LineaPedidoEntity
import com.chelita.almuerzos.datos.PedidoConLineas
import com.chelita.almuerzos.datos.PedidoEntity
import com.chelita.almuerzos.dominio.EstadoPedido
import com.chelita.almuerzos.dominio.Producto
import com.chelita.almuerzos.dominio.Producto.ALMUERZO
import com.chelita.almuerzos.dominio.Producto.PORCION_PROTEINA
import com.chelita.almuerzos.dominio.Producto.SEGUNDO
import com.chelita.almuerzos.dominio.Producto.SOPA
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class CalculadoraResumenTest {

    private val dia1 = LocalDate.of(2026, 11, 1)
    private val dia2 = LocalDate.of(2026, 11, 2)

    // Arma un pedido guardado como lo haría la app: cada línea con su precio de ese momento.
    private fun pedido(
        fecha: LocalDate,
        estado: EstadoPedido,
        vararg lineas: Triple<Producto, Int, Int>, // producto, cantidad, precio unitario
        paraLlevar: Boolean = false,
        tarrinasSegundo: Int = 0,
        tarrinasSopa: Int = 0,
        costoTarrinas: Int = 0,
    ): PedidoConLineas {
        val platos = lineas.sumOf { it.second * it.third }
        return PedidoConLineas(
            pedido = PedidoEntity(
                nombre = "Cliente",
                paraLlevar = paraLlevar,
                observaciones = "",
                estado = estado,
                fecha = fecha,
                creadoEn = 0,
                tarrinasSegundo = tarrinasSegundo,
                tarrinasSopa = tarrinasSopa,
                envasesProteina = 0,
                costoTarrinas = costoTarrinas,
                total = platos + costoTarrinas,
            ),
            lineas = lineas.map { (producto, cantidad, precio) -> LineaPedidoEntity(0, producto, cantidad, precio) },
        )
    }

    @Test
    fun `sin pedidos todo queda en cero`() {
        assertEquals(Resumen(), CalculadoraResumen.calcular(emptyList()))
    }

    @Test
    fun `suma ventas, cobros, productos, tarrinas y dias`() {
        val pedidos = listOf(
            // Día 1: 2 almuerzos para llevar, pagado: $5.00 + $0.80 de tarrinas
            pedido(
                dia1, EstadoPedido.PAGADO, Triple(ALMUERZO, 2, 250),
                paraLlevar = true, tarrinasSegundo = 2, tarrinasSopa = 2, costoTarrinas = 80,
            ),
            // Día 2: 1 segundo y 1 sopa, pagado: $2.50
            pedido(dia2, EstadoPedido.PAGADO, Triple(SEGUNDO, 1, 200), Triple(SOPA, 1, 50)),
            // Día 2: 1 almuerzo y 1 porción, todavía sin pagar: $3.25
            pedido(dia2, EstadoPedido.SERVIDO, Triple(ALMUERZO, 1, 250), Triple(PORCION_PROTEINA, 1, 75)),
        )

        val resumen = CalculadoraResumen.calcular(pedidos)

        assertEquals(3, resumen.pedidos)
        assertEquals(1, resumen.paraLlevar)
        assertEquals(580 + 250 + 325, resumen.total)
        assertEquals(580 + 250, resumen.cobrado)
        assertEquals(325, resumen.porCobrar)
        assertEquals(1, resumen.pedidosPorCobrar)
        assertEquals(
            listOf(
                VentaProducto(ALMUERZO, 3, 750),
                VentaProducto(SEGUNDO, 1, 200),
                VentaProducto(SOPA, 1, 50),
                VentaProducto(PORCION_PROTEINA, 1, 75),
            ),
            resumen.productos,
        )
        assertEquals(2, resumen.tarrinasSegundo)
        assertEquals(2, resumen.tarrinasSopa)
        assertEquals(80, resumen.costoTarrinas)
        assertEquals(listOf(VentaDia(dia2, 2, 575), VentaDia(dia1, 1, 580)), resumen.dias)
    }

    @Test
    fun `usa el precio con que se vendio cada pedido`() {
        // El almuerzo subió de $2.50 a $3.00 a mitad de mes.
        val pedidos = listOf(
            pedido(dia1, EstadoPedido.PAGADO, Triple(ALMUERZO, 1, 250)),
            pedido(dia2, EstadoPedido.PAGADO, Triple(ALMUERZO, 1, 300)),
        )

        val resumen = CalculadoraResumen.calcular(pedidos)

        assertEquals(listOf(VentaProducto(ALMUERZO, 2, 550)), resumen.productos)
        assertEquals(550, resumen.total)
    }
}
