package com.chelita.almuerzos.ui.resumen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelita.almuerzos.datos.PedidosRepositorio
import com.chelita.almuerzos.dominio.Periodo
import com.chelita.almuerzos.dominio.Rango
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class EstadoResumen(
    val cargando: Boolean = true,
    val rango: Rango? = null,
    val resumen: Resumen = Resumen(),
)

// Maneja la pantalla de resumen: elige el periodo y calcula sus totales.
class ResumenViewModel(private val repositorio: PedidosRepositorio) : ViewModel() {

    private val _periodo = MutableStateFlow(Periodo.HOY)
    val periodo: StateFlow<Periodo> = _periodo.asStateFlow()

    // Cada vez que cambia el periodo (o el día), se calculan sus fechas y se observan sus pedidos.
    @OptIn(ExperimentalCoroutinesApi::class)
    val estado: StateFlow<EstadoResumen> =
        combine(_periodo, repositorio.fechaDeHoy()) { periodo, hoy -> periodo.rango(hoy) }
            .flatMapLatest { rango ->
                repositorio.pedidosEntre(rango).map { pedidos ->
                    EstadoResumen(cargando = false, rango = rango, resumen = CalculadoraResumen.calcular(pedidos))
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoResumen())

    fun elegirPeriodo(nuevo: Periodo) {
        _periodo.value = nuevo
    }
}
