package com.chelita.almuerzos.dominio

// Lo que falta preparar para los pedidos que todavía no se sirven.
data class PorServir(
    val segundos: Int = 0, // todos los platos de segundo, incluidos los mixtos
    val mixtos: Int = 0, // de esos segundos, cuántos llevan proteína extra
    val sopas: Int = 0,
    val porciones: Int = 0, // porciones de proteína pedidas aparte
    val bebidas: Int = 0,
)

object CalculadoraDespacho {

    // Recibe las cantidades de cada pedido pendiente y suma lo que hay que servir.
    fun porServir(pedidos: List<Map<Producto, Int>>): PorServir {
        val totales = Producto.entries.associateWith { producto -> pedidos.sumOf { it[producto] ?: 0 } }
        fun suma(condicion: (Producto) -> Boolean) = totales.filterKeys(condicion).values.sum()

        return PorServir(
            segundos = suma { it.llevaSegundo },
            mixtos = suma { it.esMixto },
            sopas = suma { it.llevaSopa },
            porciones = totales.getValue(Producto.PORCION_PROTEINA),
            bebidas = suma { it.llevaBebida },
        )
    }
}
