package com.chelita.almuerzos.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chelita.almuerzos.ui.pedidos.EditarPedidoScreen
import com.chelita.almuerzos.ui.pedidos.EditarPedidoViewModel
import com.chelita.almuerzos.ui.pedidos.ListaPedidosScreen

// Las "rutas" son las direcciones de cada pantalla, como las URLs de una página web.
private object Rutas {
    const val PEDIDOS = "pedidos"
    const val PEDIDO = "pedido?${EditarPedidoViewModel.ARG_ID}={${EditarPedidoViewModel.ARG_ID}}"
    fun pedido(id: Long) = "pedido?${EditarPedidoViewModel.ARG_ID}=$id"
}

// Define qué pantalla se muestra en cada ruta y cómo se pasa de una a otra.
@Composable
fun Navegacion() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Rutas.PEDIDOS) {
        composable(Rutas.PEDIDOS) {
            ListaPedidosScreen(
                alNuevoPedido = { nav.navigate(Rutas.pedido(0)) { launchSingleTop = true } },
                alAbrirPedido = { id -> nav.navigate(Rutas.pedido(id)) { launchSingleTop = true } },
            )
        }
        composable(
            Rutas.PEDIDO,
            arguments = listOf(navArgument(EditarPedidoViewModel.ARG_ID) { type = NavType.LongType; defaultValue = 0L }),
        ) {
            EditarPedidoScreen(alVolver = { nav.popBackStack(Rutas.PEDIDOS, inclusive = false) })
        }
    }
}
