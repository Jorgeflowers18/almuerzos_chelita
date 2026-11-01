package com.chelita.almuerzos.ui.resumen

import com.chelita.almuerzos.datos.PedidoConLineas
import com.chelita.almuerzos.dominio.EstadoPedido
import com.chelita.almuerzos.dominio.Producto
import java.time.LocalDate

// Cuánto se vendió de un producto en el periodo.
data class VentaProducto(val producto: Producto, val cantidad: Int, val monto: Int)

// Los totales de un día, para la lista "día por día".
data class VentaDia(val fecha: LocalDate, val pedidos: Int, val total: Int)

data class Resumen(
    val pedidos: Int = 0,
    val paraLlevar: Int = 0, // cuántos de esos pedidos fueron para llevar
    val total: Int = 0,
    val cobrado: Int = 0,
    val porCobrar: Int = 0,
    val pedidosPorCobrar: Int = 0,
    val productos: List<VentaProducto> = emptyList(), // en el orden del menú, solo los vendidos
    val tarrinasSegundo: Int = 0,
    val tarrinasSopa: Int = 0,
    val envasesProteina: Int = 0,
    val costoTarrinas: Int = 0,
    val dias: List<VentaDia> = emptyList(), // el día más reciente primero
)

// Suma los pedidos de un periodo. Usa los montos guardados en cada pedido, así el
// resumen de meses pasados no cambia aunque después se modifiquen los precios.
object CalculadoraResumen {

    fun calcular(pedidos: List<PedidoConLineas>): Resumen {
        val datos = pedidos.map { it.pedido }
        val pendientes = datos.filter { it.estado != EstadoPedido.PAGADO }
        val lineas = pedidos.flatMap { it.lineas }

        return Resumen(
            pedidos = datos.size,
            paraLlevar = datos.count { it.paraLlevar },
            total = datos.sumOf { it.total },
            cobrado = datos.filter { it.estado == EstadoPedido.PAGADO }.sumOf { it.total },
            porCobrar = pendientes.sumOf { it.total },
            pedidosPorCobrar = pendientes.size,
            productos = lineas
                .groupBy { it.producto }
                .map { (producto, deEste) ->
                    VentaProducto(producto, deEste.sumOf { it.cantidad }, deEste.sumOf { it.cantidad * it.precioUnitario })
                }
                .sortedBy { it.producto.ordinal },
            tarrinasSegundo = datos.sumOf { it.tarrinasSegundo },
            tarrinasSopa = datos.sumOf { it.tarrinasSopa },
            envasesProteina = datos.sumOf { it.envasesProteina },
            costoTarrinas = datos.sumOf { it.costoTarrinas },
            dias = datos
                .groupBy { it.fecha }
                .map { (fecha, delDia) -> VentaDia(fecha, delDia.size, delDia.sumOf { it.total }) }
                .sortedByDescending { it.fecha },
        )
    }
}
