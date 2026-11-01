package com.chelita.almuerzos.ui.administracion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelita.almuerzos.datos.PedidosRepositorio
import com.chelita.almuerzos.dominio.Precios
import com.chelita.almuerzos.dominio.limiteDeLimpieza
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class FormularioMenu(val segundo: String = "", val sopa: String = "")

// Maneja la pantalla de administración: menú del día, precios y limpieza de datos viejos.
class AdministracionViewModel(private val repositorio: PedidosRepositorio) : ViewModel() {

    private val _menu = MutableStateFlow(FormularioMenu())
    val menu: StateFlow<FormularioMenu> = _menu.asStateFlow()

    // Lo último guardado, para activar "Guardar" solo cuando hay cambios.
    private val _menuGuardado = MutableStateFlow(FormularioMenu())
    val menuGuardado: StateFlow<FormularioMenu> = _menuGuardado.asStateFlow()

    private val _precios = MutableStateFlow(FormularioPrecios())
    val precios: StateFlow<FormularioPrecios> = _precios.asStateFlow()

    // null mientras se cargan los precios de la base de datos.
    private val _preciosGuardados = MutableStateFlow<Precios?>(null)
    val preciosGuardados: StateFlow<Precios?> = _preciosGuardados.asStateFlow()

    val pedidosAntiguos: StateFlow<Int> = repositorio.pedidosAntiguos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val limiteDeLimpieza: StateFlow<LocalDate?> = repositorio.fechaDeHoy()
        .map { limiteDeLimpieza(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Avisos cortos para la mesera ("Precios guardados"). Un Channel entrega cada aviso una sola vez.
    private val _mensajes = Channel<String>(Channel.BUFFERED)
    val mensajes: Flow<String> = _mensajes.receiveAsFlow()

    init {
        viewModelScope.launch {
            val guardados = repositorio.precios.first()
            _precios.value = FormularioPrecios.desde(guardados)
            _preciosGuardados.value = guardados

            val menuDeHoy = repositorio.menuDeHoy().first()
            val formulario = FormularioMenu(menuDeHoy?.segundo ?: "", menuDeHoy?.sopa ?: "")
            _menu.value = formulario
            _menuGuardado.value = formulario
        }
    }

    fun cambiarMenu(cambio: (FormularioMenu) -> FormularioMenu) = _menu.update(cambio)

    fun cambiarPrecio(campo: CampoPrecio, texto: String) = _precios.update { it.cambiar(campo, texto) }

    fun cambiarPorciones(texto: String) = _precios.update { it.copy(porcionesPorEnvase = texto) }

    fun guardarMenu() {
        val formulario = _menu.value
        viewModelScope.launch {
            repositorio.guardarMenu(formulario.segundo, formulario.sopa)
            _menuGuardado.value = formulario
            _mensajes.send("Menú de hoy guardado")
        }
    }

    fun guardarPrecios() {
        val nuevos = _precios.value.aPrecios() ?: return
        viewModelScope.launch {
            repositorio.guardarPrecios(nuevos)
            _preciosGuardados.value = nuevos
            // Se vuelve a escribir el formulario con formato uniforme: "3" pasa a "3.00".
            _precios.value = FormularioPrecios.desde(nuevos)
            _mensajes.send("Precios guardados")
        }
    }

    fun borrarAntiguos() {
        viewModelScope.launch {
            val borrados = repositorio.borrarAntiguos()
            _mensajes.send(if (borrados == 1) "Se borró 1 pedido antiguo" else "Se borraron $borrados pedidos antiguos")
        }
    }
}
