package com.chelita.almuerzos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelita.almuerzos.datos.PedidosRepositorio
import com.chelita.almuerzos.dominio.EstadoPedido
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Lo que necesita la barra de abajo: cuántos pedidos faltan por servir, para el globito.
class NavegacionViewModel(repositorio: PedidosRepositorio) : ViewModel() {
    val pendientes: StateFlow<Int> = repositorio.pedidosDeHoy()
        .map { pedidos -> pedidos.count { it.pedido.estado == EstadoPedido.PENDIENTE } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
