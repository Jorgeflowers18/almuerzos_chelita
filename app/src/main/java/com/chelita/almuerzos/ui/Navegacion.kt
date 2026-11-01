package com.chelita.almuerzos.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chelita.almuerzos.ui.cobro.CobroScreen
import com.chelita.almuerzos.ui.cobro.CobroViewModel
import com.chelita.almuerzos.ui.despacho.DespachoScreen
import com.chelita.almuerzos.ui.pedidos.EditarPedidoScreen
import com.chelita.almuerzos.ui.pedidos.EditarPedidoViewModel
import com.chelita.almuerzos.ui.pedidos.ListaPedidosScreen

// Las "rutas" son las direcciones de cada pantalla, como las URLs de una página web.
private object Rutas {
    const val PEDIDOS = "pedidos"
    const val POR_SERVIR = "por_servir"
    const val PEDIDO = "pedido?${EditarPedidoViewModel.ARG_ID}={${EditarPedidoViewModel.ARG_ID}}"
    const val COBRO = "cobro/{${CobroViewModel.ARG_ID}}"
    fun pedido(id: Long) = "pedido?${EditarPedidoViewModel.ARG_ID}=$id"
    fun cobro(id: Long) = "cobro/$id"
}

// Las secciones de la barra de abajo. Las pantallas que no están aquí (editar, cobrar)
// se muestran sin la barra, a pantalla completa.
private enum class Seccion(val ruta: String, val etiqueta: String, val icono: ImageVector) {
    PEDIDOS(Rutas.PEDIDOS, "Pedidos", Icons.AutoMirrored.Filled.List),
    POR_SERVIR(Rutas.POR_SERVIR, "Por servir", Icons.Default.Notifications),
}

// Define qué pantalla se muestra en cada ruta y cómo se pasa de una a otra.
@Composable
fun Navegacion(viewModel: NavegacionViewModel = viewModel(factory = FabricaViewModels)) {
    val nav = rememberNavController()
    val entradaActual by nav.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route
    val pendientes by viewModel.pendientes.collectAsStateWithLifecycle()

    val abrirPedido = { id: Long -> nav.navigate(Rutas.pedido(id)) { launchSingleTop = true } }
    val abrirCobro = { id: Long -> nav.navigate(Rutas.cobro(id)) { launchSingleTop = true } }

    Scaffold(
        // Cada pantalla maneja sus propios márgenes (barra de estado, teclado); aquí solo
        // se reserva el espacio de la barra de abajo.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (Seccion.entries.any { it.ruta == rutaActual }) {
                BarraSecciones(rutaActual, pendientes) { seccion -> nav.irASeccion(seccion) }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Rutas.PEDIDOS,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable(Rutas.PEDIDOS) {
                ListaPedidosScreen(
                    alNuevoPedido = { abrirPedido(0) },
                    alAbrirPedido = abrirPedido,
                    alCobrar = abrirCobro,
                )
            }
            composable(Rutas.POR_SERVIR) {
                DespachoScreen(alAbrirPedido = abrirPedido)
            }
            composable(
                Rutas.PEDIDO,
                arguments = listOf(navArgument(EditarPedidoViewModel.ARG_ID) { type = NavType.LongType; defaultValue = 0L }),
            ) { entrada ->
                EditarPedidoScreen(alVolver = { nav.volverDesde(entrada) })
            }
            composable(
                Rutas.COBRO,
                arguments = listOf(navArgument(CobroViewModel.ARG_ID) { type = NavType.LongType }),
            ) { entrada ->
                CobroScreen(alVolver = { nav.volverDesde(entrada) })
            }
        }
    }
}

@Composable
private fun BarraSecciones(rutaActual: String?, pendientes: Int, alElegir: (Seccion) -> Unit) {
    NavigationBar {
        Seccion.entries.forEach { seccion ->
            NavigationBarItem(
                selected = seccion.ruta == rutaActual,
                onClick = { alElegir(seccion) },
                icon = {
                    // El globito con el número de pedidos que faltan por servir.
                    BadgedBox(badge = { if (seccion == Seccion.POR_SERVIR && pendientes > 0) Badge { Text("$pendientes") } }) {
                        Icon(seccion.icono, contentDescription = null)
                    }
                },
                label = { Text(seccion.etiqueta) },
            )
        }
    }
}

// Cambia de sección sin apilar pantallas: "atrás" desde cualquier sección vuelve a Pedidos
// y luego sale de la app. Cada sección recuerda dónde estaba (por ejemplo, el filtro elegido).
private fun NavController.irASeccion(seccion: Seccion) {
    navigate(seccion.ruta) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// Vuelve a la pantalla anterior solo si esta pantalla sigue activa: así dos toques seguidos
// en "Guardar" o en la flecha no sacan a la mesera de la app.
private fun NavController.volverDesde(entrada: NavBackStackEntry) {
    if (entrada.lifecycle.currentState == Lifecycle.State.RESUMED) popBackStack()
}
