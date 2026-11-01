package com.chelita.almuerzos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chelita.almuerzos.datos.PedidosRepositorio
import com.chelita.almuerzos.dominio.Producto
import com.chelita.almuerzos.dominio.comoDinero
import com.chelita.almuerzos.ui.theme.AlmuerzosTheme
import kotlinx.coroutines.launch

// Punto de entrada: Android crea esta Activity al abrir la app.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ajusta la barra de estado (hora, batería) al fondo claro de la app.
        enableEdgeToEdge()
        val repositorio = (application as AlmuerzosApp).repositorio
        // setContent dibuja la interfaz usando Jetpack Compose.
        setContent {
            AlmuerzosTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PantallaPruebaDatos(repositorio)
                }
            }
        }
    }
}

// PANTALLA PROVISIONAL de la fase 3: sirve para comprobar que los pedidos se guardan
// y siguen ahí después de cerrar la app. Se reemplaza en la fase 4.
@Composable
fun PantallaPruebaDatos(repositorio: PedidosRepositorio) {
    // "remember" evita crear la consulta de nuevo cada vez que se redibuja la pantalla.
    // "collectAsState" convierte el Flow en un valor que, al cambiar, redibuja la pantalla.
    val pedidos by remember { repositorio.pedidosDeHoy() }.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Prueba de base de datos", style = MaterialTheme.typography.headlineSmall)
        Text("Pedidos de hoy: ${pedidos.size}")
        Button(
            onClick = {
                // Las funciones suspend se llaman dentro de "launch" (en segundo plano).
                scope.launch {
                    repositorio.guardarPedido(
                        nombre = "Cliente ${pedidos.size + 1}",
                        paraLlevar = pedidos.size % 2 == 0,
                        cantidades = mapOf(Producto.ALMUERZO to 1, Producto.SOPA to pedidos.size % 2),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Agregar pedido de prueba")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(pedidos, key = { it.pedido.id }) { item ->
                val p = item.pedido
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${p.nombre} · ${if (p.paraLlevar) "Llevar" else "Aquí"} · ${p.estado.nombre}")
                    Text(p.total.comoDinero())
                }
            }
        }
    }
}
