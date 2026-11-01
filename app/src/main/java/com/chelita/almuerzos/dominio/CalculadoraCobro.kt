package com.chelita.almuerzos.dominio

// Una fila del desglose: "2 x Almuerzo a $2.50 = $5.00".
data class LineaCobro(
    val producto: Producto,
    val cantidad: Int,
    val precioUnitario: Int,
) {
    val subtotal: Int get() = cantidad * precioUnitario
}

// Resultado completo del cálculo de un pedido. Cantidades en unidades, dinero en centavos.
data class Cobro(
    val lineas: List<LineaCobro>,
    val tarrinasSegundo: Int,
    val tarrinasSopa: Int,
    val envasesProteina: Int,
    val costoTarrinas: Int,
) {
    val subtotalPlatos: Int get() = lineas.sumOf { it.subtotal }
    val total: Int get() = subtotalPlatos + costoTarrinas
}

// "object" crea un único objeto (no hace falta instanciarlo): CalculadoraCobro.calcular(...)
object CalculadoraCobro {

    /**
     * Calcula el cobro de un pedido.
     *
     * @param cantidades cuántas unidades de cada producto se pidieron. Ej: {ALMUERZO=2, SOPA=1}
     * @param paraLlevar si es true se cobran las tarrinas.
     * @param precios precios vigentes.
     */
    fun calcular(cantidades: Map<Producto, Int>, paraLlevar: Boolean, precios: Precios): Cobro {
        require(cantidades.values.all { it >= 0 }) { "Las cantidades no pueden ser negativas" }

        // Una línea por cada producto pedido, en el orden en que están declarados en el enum.
        val lineas = Producto.entries
            .filter { (cantidades[it] ?: 0) > 0 }
            .map { LineaCobro(it, cantidades.getValue(it), precios.precioDe(it)) }

        if (!paraLlevar) {
            return Cobro(lineas, tarrinasSegundo = 0, tarrinasSopa = 0, envasesProteina = 0, costoTarrinas = 0)
        }

        // Para llevar: una tarrina por cada segundo y una pequeña por cada sopa.
        // El mixto va en la misma tarrina del segundo, por eso no suma nada extra.
        val tarrinasSegundo = lineas.filter { it.producto.llevaSegundo }.sumOf { it.cantidad }
        val tarrinasSopa = lineas.filter { it.producto.llevaSopa }.sumOf { it.cantidad }

        // Las porciones de proteína van juntas en envases pequeños: hasta 3 por envase.
        // División redondeando hacia arriba: 1-3 porciones = 1 envase, 4-6 = 2, etc.
        val porciones = cantidades[Producto.PORCION_PROTEINA] ?: 0
        val envasesProteina = (porciones + precios.porcionesPorEnvase - 1) / precios.porcionesPorEnvase

        val costoTarrinas = tarrinasSegundo * precios.tarrinaSegundo +
            tarrinasSopa * precios.tarrinaSopa +
            envasesProteina * precios.envaseProteina

        return Cobro(lineas, tarrinasSegundo, tarrinasSopa, envasesProteina, costoTarrinas)
    }
}
