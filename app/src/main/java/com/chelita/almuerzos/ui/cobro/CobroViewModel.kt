package com.chelita.almuerzos.ui.cobro

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelita.almuerzos.datos.PedidoConLineas
import com.chelita.almuerzos.datos.PedidosRepositorio
import com.chelita.almuerzos.dominio.EstadoPedido
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// pedido = null cuando ya cargó pero el pedido no existe (por ejemplo, se eliminó).
data class EstadoCobro(val cargando: Boolean = true, val pedido: PedidoConLineas? = null)

// Maneja la pantalla de cobro de un pedido. El id llega desde la navegación.
class CobroViewModel(
    private val repositorio: PedidosRepositorio,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val pedidoId: Long = savedStateHandle.get<Long>(ARG_ID) ?: 0L

    // Se observa el pedido: si alguien lo edita, el cobro se actualiza solo.
    val estado: StateFlow<EstadoCobro> = repositorio.observar(pedidoId)
        .map { EstadoCobro(cargando = false, pedido = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoCobro())

    // Lo que escribe la mesera en "Monto recibido". Va aparte para que el campo de texto
    // responda al instante mientras se escribe.
    private val _recibido = MutableStateFlow("")
    val recibido: StateFlow<String> = _recibido.asStateFlow()

    private var pagando = false

    fun cambiarRecibido(texto: String) {
        _recibido.value = texto
    }

    fun marcarPagado(alTerminar: () -> Unit) {
        if (pagando) return
        pagando = true
        viewModelScope.launch {
            repositorio.cambiarEstado(pedidoId, EstadoPedido.PAGADO)
            alTerminar()
        }
    }

    companion object {
        const val ARG_ID = "id"
    }
}
