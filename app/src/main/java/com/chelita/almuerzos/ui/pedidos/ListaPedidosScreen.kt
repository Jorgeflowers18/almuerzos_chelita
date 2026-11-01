package com.chelita.almuerzos.ui.pedidos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.chelita.almuerzos.datos.MenuDiaEntity
import com.chelita.almuerzos.datos.PedidoConLineas
import com.chelita.almuerzos.dominio.EstadoPedido
import com.chelita.almuerzos.dominio.comoDinero
import com.chelita.almuerzos.ui.EtiquetaEstado
import com.chelita.almuerzos.ui.EtiquetaParaLlevar
import com.chelita.almuerzos.ui.FabricaViewModels
import com.chelita.almuerzos.ui.fechaLarga
import com.chelita.almuerzos.ui.resumenProductos
import java.time.LocalDate

// Pantalla principal: los pedidos del día.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaPedidosScreen(
    alNuevoPedido: () -> Unit,
    alAbrirPedido: (Long) -> Unit,
    alCobrar: (Long) -> Unit,
    alConfigurarRespaldos: () -> Unit,
    viewModel: ListaPedidosViewModel = viewModel(factory = FabricaViewModels),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Pedidos de hoy")
                        Text(fechaLarga(LocalDate.now()), style = MaterialTheme.typography.bodyMedium)
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = alNuevoPedido,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuevo pedido") },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (estado.sinRespaldos && !estado.cargando) AvisoSinRespaldos(alConfigurarRespaldos)
            estado.menu?.let { MenuDeHoy(it) }
            FiltrosEstado(estado.filtro, estado.conteos, viewModel::elegirFiltro)

            when {
                estado.cargando -> Unit
                estado.pedidos.isEmpty() -> MensajeVacio(estado.filtro)
                else -> LazyColumn(
                    // Espacio abajo para que el botón "Nuevo pedido" no tape la última tarjeta.
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(estado.pedidos, key = { it.pedido.id }) { pedido ->
                        TarjetaPedido(
                            pedido = pedido,
                            alTocar = { alAbrirPedido(pedido.pedido.id) },
                            alCobrar = { alCobrar(pedido.pedido.id) },
                            alAvanzarEstado = { viewModel.avanzarEstado(pedido) },
                        )
                    }
                }
            }
        }
    }
}

// Aviso rojo mientras no haya carpeta de respaldos: sin ella, los datos solo viven en este celular.
@Composable
private fun AvisoSinRespaldos(alConfigurar: () -> Unit) {
    Surface(
        onClick = alConfigurar,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            "Los respaldos no están activados. Toca aquí para elegir la carpeta de respaldos.",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

// "Segundo: Seco de pollo · Sopa: Locro de papa", lo que se haya anotado en Ajustes.
@Composable
private fun MenuDeHoy(menu: MenuDiaEntity) {
    val partes = buildList {
        if (menu.segundo.isNotBlank()) add("Segundo: ${menu.segundo}")
        if (menu.sopa.isNotBlank()) add("Sopa: ${menu.sopa}")
    }
    if (partes.isEmpty()) return
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            partes.joinToString(" · "),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun FiltrosEstado(
    seleccionado: FiltroPedidos,
    conteos: Map<FiltroPedidos, Int>,
    alElegir: (FiltroPedidos) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        FiltroPedidos.entries.forEach { filtro ->
            FilterChip(
                selected = filtro == seleccionado,
                onClick = { alElegir(filtro) },
                label = { Text("${filtro.etiqueta} (${conteos[filtro] ?: 0})") },
            )
        }
    }
}

@Composable
private fun TarjetaPedido(
    pedido: PedidoConLineas,
    alTocar: () -> Unit,
    alCobrar: () -> Unit,
    alAvanzarEstado: () -> Unit,
) {
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
                Text(p.total.comoDinero(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EtiquetaEstado(p.estado, p.paraLlevar)
                if (p.paraLlevar) EtiquetaParaLlevar()
            }
            Text(resumenProductos(pedido), style = MaterialTheme.typography.bodyLarge)
            if (p.observaciones.isNotBlank()) {
                Text(
                    p.observaciones,
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = alCobrar, modifier = Modifier.weight(1f)) {
                    Text(if (p.estado == EstadoPedido.PAGADO) "Ver cobro" else "Cobrar")
                }
                p.estado.siguiente?.let { siguiente ->
                    Button(onClick = alAvanzarEstado, modifier = Modifier.weight(1f)) {
                        Text("Marcar ${siguiente.nombrePara(p.paraLlevar).lowercase()}")
                    }
                }
            }
        }
    }
}

@Composable
private fun MensajeVacio(filtro: FiltroPedidos) {
    val texto = if (filtro == FiltroPedidos.TODOS) {
        "Todavía no hay pedidos hoy.\nToca \"Nuevo pedido\" para empezar."
    } else {
        "No hay pedidos ${filtro.etiqueta.lowercase()}."
    }
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            texto,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
