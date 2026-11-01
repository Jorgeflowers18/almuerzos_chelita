package com.chelita.almuerzos.ui.despacho

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chelita.almuerzos.datos.PedidoConLineas
import com.chelita.almuerzos.dominio.EstadoPedido
import com.chelita.almuerzos.dominio.PorServir
import com.chelita.almuerzos.ui.EtiquetaParaLlevar
import com.chelita.almuerzos.ui.FabricaViewModels
import com.chelita.almuerzos.ui.FondoParaLlevar
import com.chelita.almuerzos.ui.TextoParaLlevar
import com.chelita.almuerzos.ui.horaDe
import com.chelita.almuerzos.ui.resumenProductos

// Pantalla "Por servir": cuánto hay que preparar y qué pedidos esperan, el más antiguo primero.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DespachoScreen(
    alAbrirPedido: (Long) -> Unit,
    viewModel: DespachoViewModel = viewModel(factory = FabricaViewModels),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Por servir")
                        Text(textoPendientes(estado.pendientes.size), style = MaterialTheme.typography.bodyMedium)
                    }
                },
            )
        },
    ) { padding ->
        when {
            estado.cargando -> Unit
            estado.pendientes.isEmpty() -> TodoServido(Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item { Totales(estado.porServir) }
                if (estado.hayTarrinas) {
                    item { AvisoTarrinas(estado) }
                }
                item {
                    Text(
                        "En orden de llegada",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(estado.pendientes, key = { it.pedido.id }) { pedido ->
                    TarjetaPendiente(
                        pedido = pedido,
                        alTocar = { alAbrirPedido(pedido.pedido.id) },
                        alServir = { viewModel.marcarServido(pedido) },
                    )
                }
            }
        }
    }
}

// Los números grandes, en dos filas de dos casillas.
@Composable
private fun Totales(porServir: PorServir) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Casilla(
                "Segundos",
                porServir.segundos,
                if (porServir.mixtos > 0) "${porServir.mixtos} mixto${if (porServir.mixtos > 1) "s" else ""}" else null,
                Modifier.weight(1f),
            )
            Casilla("Sopas", porServir.sopas, null, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Casilla("Bebidas", porServir.bebidas, null, Modifier.weight(1f))
            Casilla("Porciones extra", porServir.porciones, null, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Casilla(titulo: String, numero: Int, detalle: String?, modifier: Modifier) {
    val activa = numero > 0
    Surface(
        color = if (activa) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (activa) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text("$numero", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            // Siempre ocupa su línea, para que todas las casillas midan lo mismo.
            Text(detalle ?: " ", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AvisoTarrinas(estado: EstadoDespacho) {
    val partes = buildList {
        if (estado.tarrinasSegundo > 0) add("${estado.tarrinasSegundo} de segundo")
        if (estado.tarrinasSopa > 0) add("${estado.tarrinasSopa} de sopa")
        if (estado.envasesProteina > 0) add("${estado.envasesProteina} de proteína")
    }
    Surface(color = FondoParaLlevar, contentColor = TextoParaLlevar, shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Para llevar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Alistar tarrinas: ${partes.joinToString(", ")}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun TarjetaPendiente(pedido: PedidoConLineas, alTocar: () -> Unit, alServir: () -> Unit) {
    val p = pedido.pedido
    Card(
        onClick = alTocar,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    p.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    horaDe(p.creadoEn),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (p.paraLlevar) EtiquetaParaLlevar()
            Text(resumenProductos(pedido), style = MaterialTheme.typography.titleMedium)
            if (p.observaciones.isNotBlank()) {
                Text(
                    p.observaciones,
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(onClick = alServir, modifier = Modifier.fillMaxWidth()) {
                Text("Marcar ${EstadoPedido.SERVIDO.nombrePara(p.paraLlevar).lowercase()}")
            }
        }
    }
}

@Composable
private fun TodoServido(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            "No hay nada pendiente.\nTodos los pedidos están servidos.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun textoPendientes(cantidad: Int): String = when (cantidad) {
    0 -> "Nada pendiente"
    1 -> "1 pedido pendiente"
    else -> "$cantidad pedidos pendientes"
}
