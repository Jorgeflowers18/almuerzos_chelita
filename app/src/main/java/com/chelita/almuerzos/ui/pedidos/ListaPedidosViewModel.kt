package com.chelita.almuerzos.ui.pedidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelita.almuerzos.datos.MenuDiaEntity
import com.chelita.almuerzos.datos.PedidoConLineas
import com.chelita.almuerzos.datos.PedidosRepositorio
import com.chelita.almuerzos.dominio.EstadoPedido
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Los filtros que aparecen arriba de la lista. "estado = null" significa todos.
enum class FiltroPedidos(val etiqueta: String, val estado: EstadoPedido?) {
    TODOS("Todos", null),
    PENDIENTES("Pendientes", EstadoPedido.PENDIENTE),
    SERVIDOS("Servidos", EstadoPedido.SERVIDO),
    PAGADOS("Pagados", EstadoPedido.PAGADO),
}

// Todo lo que la pantalla de lista necesita para dibujarse.
data class EstadoListaPedidos(
    val cargando: Boolean = true,
    val pedidos: List<PedidoConLineas> = emptyList(),
    val filtro: FiltroPedidos = FiltroPedidos.TODOS,
    val conteos: Map<FiltroPedidos, Int> = emptyMap(),
    val menu: MenuDiaEntity? = null,
)

// Un ViewModel sobrevive mientras la pantalla exista y guarda su estado.
// La pantalla solo lo observa y le avisa de las acciones de la mesera.
class ListaPedidosViewModel(private val repositorio: PedidosRepositorio) : ViewModel() {

    private val filtro = MutableStateFlow(FiltroPedidos.TODOS)

    // "combine" junta varios flujos: cada vez que cambian los pedidos, el filtro o el menú,
    // se recalcula lo que muestra la pantalla.
    val estado: StateFlow<EstadoListaPedidos> =
        combine(repositorio.pedidosDeHoy(), filtro, repositorio.menuDeHoy()) { pedidos, filtroActual, menu ->
            EstadoListaPedidos(
                cargando = false,
                pedidos = pedidos.filter { filtroActual.estado == null || it.pedido.estado == filtroActual.estado },
                filtro = filtroActual,
                conteos = FiltroPedidos.entries.associateWith { f ->
                    pedidos.count { f.estado == null || it.pedido.estado == f.estado }
                },
                menu = menu,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoListaPedidos())

    fun elegirFiltro(nuevo: FiltroPedidos) {
        filtro.value = nuevo
    }

    fun avanzarEstado(pedido: PedidoConLineas) {
        val siguiente = pedido.pedido.estado.siguiente ?: return
        viewModelScope.launch { repositorio.cambiarEstado(pedido.pedido.id, siguiente) }
    }
}
