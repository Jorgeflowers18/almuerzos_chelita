package com.chelita.almuerzos.ui.administracion

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chelita.almuerzos.datos.ContenidoRespaldo
import com.chelita.almuerzos.datos.PedidosRepositorio
import com.chelita.almuerzos.datos.respaldo.ErrorDeRespaldo
import com.chelita.almuerzos.datos.respaldo.EstadoRespaldos
import com.chelita.almuerzos.datos.respaldo.GestorRespaldos
import com.chelita.almuerzos.datos.respaldo.TipoRespaldo
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

// Un respaldo leído que espera la confirmación de la mesera antes de restaurarse.
data class PropuestaRestauracion(val contenido: ContenidoRespaldo, val pedidosActuales: Int)

// Maneja la pantalla de administración: menú del día, precios, respaldos y limpieza de datos viejos.
class AdministracionViewModel(
    private val repositorio: PedidosRepositorio,
    private val respaldos: GestorRespaldos,
) : ViewModel() {

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

    val respaldo: StateFlow<EstadoRespaldos> = respaldos.estado

    private val _propuesta = MutableStateFlow<PropuestaRestauracion?>(null)
    val propuesta: StateFlow<PropuestaRestauracion?> = _propuesta.asStateFlow()

    // Avisos cortos para la mesera ("Precios guardados"). Un Channel entrega cada aviso una sola vez.
    private val _mensajes = Channel<String>(Channel.BUFFERED)
    val mensajes: Flow<String> = _mensajes.receiveAsFlow()

    // El archivo listo para enviar; la pantalla abre el menú de "Compartir" de Android.
    private val _envios = Channel<Uri>(Channel.BUFFERED)
    val envios: Flow<Uri> = _envios.receiveAsFlow()

    init {
        viewModelScope.launch { cargarFormularios() }
    }

    private suspend fun cargarFormularios() {
        val guardados = repositorio.precios.first()
        _precios.value = FormularioPrecios.desde(guardados)
        _preciosGuardados.value = guardados

        val menuDeHoy = repositorio.menuDeHoy().first()
        val formulario = FormularioMenu(menuDeHoy?.segundo ?: "", menuDeHoy?.sopa ?: "")
        _menu.value = formulario
        _menuGuardado.value = formulario
    }

    fun cambiarMenu(cambio: (FormularioMenu) -> FormularioMenu) = _menu.update(cambio)

    fun cambiarPrecio(campo: CampoPrecio, texto: String) = _precios.update { it.cambiar(campo, texto) }

    fun cambiarPorciones(texto: String) = _precios.update { it.copy(porcionesPorEnvase = texto) }

    fun guardarMenu() {
        val formulario = _menu.value
        conAviso {
            repositorio.guardarMenu(formulario.segundo, formulario.sopa)
            _menuGuardado.value = formulario
            "Menú de hoy guardado"
        }
    }

    fun guardarPrecios() {
        val nuevos = _precios.value.aPrecios() ?: return
        conAviso {
            repositorio.guardarPrecios(nuevos)
            _preciosGuardados.value = nuevos
            // Se vuelve a escribir el formulario con formato uniforme: "3" pasa a "3.00".
            _precios.value = FormularioPrecios.desde(nuevos)
            "Precios guardados"
        }
    }

    fun borrarAntiguos() = conAviso {
        val borrados = respaldos.limpiarConRespaldo()
        if (borrados == 1) "Se respaldó y se borró 1 pedido antiguo" else "Se respaldaron y se borraron $borrados pedidos antiguos"
    }

    fun elegirCarpeta(uri: Uri) = conAviso {
        val encontrado = respaldos.elegirCarpeta(uri)
        if (encontrado != null) {
            // El celular está vacío y la carpeta ya tenía un respaldo: se ofrece restaurarlo.
            _propuesta.value = PropuestaRestauracion(encontrado, pedidosActuales = 0)
            null
        } else {
            "Carpeta de respaldos lista"
        }
    }

    fun respaldarAhora() = conAviso {
        respaldos.respaldar(TipoRespaldo.AUTOMATICO)
        "Respaldo guardado"
    }

    fun enviarRespaldo() = conAviso {
        _envios.send(respaldos.archivoParaEnviar())
        null
    }

    fun prepararRestauracion(uri: Uri) = conAviso {
        _propuesta.value = PropuestaRestauracion(respaldos.leerArchivo(uri), respaldos.pedidosEnEsteCelular())
        null
    }

    fun confirmarRestauracion() {
        val propuesta = _propuesta.value ?: return
        _propuesta.value = null
        conAviso {
            respaldos.restaurar(propuesta.contenido)
            cargarFormularios()
            "Datos restaurados: ${propuesta.contenido.pedidos.size} pedidos"
        }
    }

    fun cancelarRestauracion() {
        _propuesta.value = null
    }

    // Ejecuta una acción en segundo plano y muestra el aviso que devuelve (o el error, si falla).
    private fun conAviso(accion: suspend () -> String?) {
        viewModelScope.launch {
            val mensaje = try {
                accion()
            } catch (e: ErrorDeRespaldo) {
                e.message
            } catch (e: Exception) {
                "Algo salió mal: ${e.message}"
            }
            mensaje?.let { _mensajes.send(it) }
        }
    }
}
