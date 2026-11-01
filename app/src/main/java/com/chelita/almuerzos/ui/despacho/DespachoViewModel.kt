package com.chelita.almuerzos.ui.despacho

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelita.almuerzos.datos.PedidoConLineas
import com.chelita.almuerzos.datos.PedidosRepositorio
import com.chelita.almuerzos.dominio.CalculadoraDespacho
import com.chelita.almuerzos.dominio.EstadoPedido
import com.chelita.almuerzos.dominio.PorServir
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EstadoDespacho(
    val cargando: Boolean = true,
    val pendientes: List<PedidoConLineas> = emptyList(), // en orden de llegada
    val porServir: PorServir = PorServir(),
    // Envases que hay que alistar para los pedidos para llevar pendientes.
    val tarrinasSegundo: Int = 0,
    val tarrinasSopa: Int = 0,
    val envasesProteina: Int = 0,
) {
    val hayTarrinas: Boolean get() = tarrinasSegundo + tarrinasSopa + envasesProteina > 0
}

// Maneja la pantalla "Por servir": solo mira los pedidos pendientes de hoy.
class DespachoViewModel(private val repositorio: PedidosRepositorio) : ViewModel() {

    val estado: StateFlow<EstadoDespacho> = repositorio.pedidosDeHoy()
        .map { pedidos ->
            val pendientes = pedidos.filter { it.pedido.estado == EstadoPedido.PENDIENTE }
            val paraLlevar = pendientes.map { it.pedido }.filter { it.paraLlevar }
            EstadoDespacho(
                cargando = false,
                pendientes = pendientes,
                porServir = CalculadoraDespacho.porServir(pendientes.map { it.cantidades }),
                tarrinasSegundo = paraLlevar.sumOf { it.tarrinasSegundo },
                tarrinasSopa = paraLlevar.sumOf { it.tarrinasSopa },
                envasesProteina = paraLlevar.sumOf { it.envasesProteina },
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoDespacho())

    fun marcarServido(pedido: PedidoConLineas) {
        viewModelScope.launch { repositorio.cambiarEstado(pedido.pedido.id, EstadoPedido.SERVIDO) }
    }
}
