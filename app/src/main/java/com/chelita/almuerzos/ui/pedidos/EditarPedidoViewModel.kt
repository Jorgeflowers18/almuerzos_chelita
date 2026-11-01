package com.chelita.almuerzos.ui.pedidos

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelita.almuerzos.datos.PedidosRepositorio
import com.chelita.almuerzos.dominio.Precios
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Maneja la pantalla de nuevo pedido / editar pedido.
// El id del pedido llega desde la navegación (0 = pedido nuevo).
class EditarPedidoViewModel(
    private val repositorio: PedidosRepositorio,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val pedidoId: Long = savedStateHandle.get<Long>(ARG_ID) ?: 0L
    val esNuevo: Boolean get() = pedidoId == 0L

    private val _formulario = MutableStateFlow(FormularioPedido())
    val formulario: StateFlow<FormularioPedido> = _formulario.asStateFlow()

    // Un pedido nuevo está listo de inmediato; uno existente, cuando termina de cargarse.
    private val _listo = MutableStateFlow(esNuevo)
    val listo: StateFlow<Boolean> = _listo.asStateFlow()

    val precios: StateFlow<Precios> = repositorio.precios
        .stateIn(viewModelScope, SharingStarted.Eagerly, Precios())

    // Cómo estaba el formulario al abrir la pantalla, para saber si hay cambios sin guardar.
    private var original = FormularioPedido()

    // Evita guardar dos veces si la mesera toca "Guardar" dos veces seguidas.
    private var guardando = false

    init {
        if (!esNuevo) {
            viewModelScope.launch {
                repositorio.obtener(pedidoId)?.let {
                    original = FormularioPedido.desde(it)
                    _formulario.value = original
                }
                _listo.value = true
            }
        }
    }

    // Recibe una función que transforma el formulario, por ejemplo: { it.sumar(ALMUERZO) }
    fun actualizar(cambio: (FormularioPedido) -> FormularioPedido) {
        _formulario.update(cambio)
    }

    fun hayCambios(): Boolean = _formulario.value != original

    fun guardar(alTerminar: () -> Unit) {
        val f = _formulario.value
        if (!f.sePuedeGuardar || guardando) return
        guardando = true
        viewModelScope.launch {
            repositorio.guardarPedido(
                nombre = f.nombre,
                paraLlevar = f.paraLlevar,
                cantidades = f.cantidades,
                observaciones = f.observaciones,
                id = pedidoId,
                estado = f.estado,
            )
            alTerminar()
        }
    }

    fun eliminar(alTerminar: () -> Unit) {
        viewModelScope.launch {
            repositorio.eliminar(pedidoId)
            alTerminar()
        }
    }

    companion object {
        const val ARG_ID = "id"
    }
}
