package com.chelita.almuerzos.ui.pedidos

import com.chelita.almuerzos.datos.PedidoConLineas
import com.chelita.almuerzos.dominio.CalculadoraCobro
import com.chelita.almuerzos.dominio.Cobro
import com.chelita.almuerzos.dominio.EstadoPedido
import com.chelita.almuerzos.dominio.Precios
import com.chelita.almuerzos.dominio.Producto

// Lo que la mesera va llenando en la pantalla de pedido, antes de guardarlo.
// Es inmutable: cada cambio crea una copia nueva con "copy(...)". Así Compose detecta
// el cambio y redibuja la pantalla.
data class FormularioPedido(
    val nombre: String = "",
    val paraLlevar: Boolean = false,
    val cantidades: Map<Producto, Int> = emptyMap(),
    val observaciones: String = "",
    val estado: EstadoPedido = EstadoPedido.PENDIENTE,
) {
    fun cantidad(producto: Producto): Int = cantidades[producto] ?: 0

    fun sumar(producto: Producto) = copy(cantidades = cantidades + (producto to cantidad(producto) + 1))

    // Nunca baja de cero.
    fun restar(producto: Producto) = copy(cantidades = cantidades + (producto to maxOf(0, cantidad(producto) - 1)))

    val nombreValido: Boolean get() = nombre.isNotBlank()
    val tieneProductos: Boolean get() = cantidades.values.any { it > 0 }
    val sePuedeGuardar: Boolean get() = nombreValido && tieneProductos

    fun cobro(precios: Precios): Cobro = CalculadoraCobro.calcular(cantidades, paraLlevar, precios)

    companion object {
        // Llena el formulario con un pedido ya guardado, para editarlo.
        fun desde(guardado: PedidoConLineas) = FormularioPedido(
            nombre = guardado.pedido.nombre,
            paraLlevar = guardado.pedido.paraLlevar,
            cantidades = guardado.cantidades,
            observaciones = guardado.pedido.observaciones,
            estado = guardado.pedido.estado,
        )
    }
}
