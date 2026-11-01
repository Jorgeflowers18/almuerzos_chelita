package com.chelita.almuerzos.ui.cobro

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chelita.almuerzos.datos.PedidoConLineas
import com.chelita.almuerzos.dominio.EstadoPedido
import com.chelita.almuerzos.dominio.aCentavos
import com.chelita.almuerzos.dominio.comoDinero
import com.chelita.almuerzos.dominio.comoMonto
import com.chelita.almuerzos.dominio.montosSugeridos
import com.chelita.almuerzos.ui.EtiquetaEstado
import com.chelita.almuerzos.ui.EtiquetaParaLlevar
import com.chelita.almuerzos.ui.FabricaViewModels
import com.chelita.almuerzos.ui.textoCobro
import com.chelita.almuerzos.ui.textoTarrinas

// Pantalla de cobro: el desglose del pedido, el cálculo del vuelto y el botón de pagado.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CobroScreen(
    alVolver: () -> Unit,
    viewModel: CobroViewModel = viewModel(factory = FabricaViewModels),
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val recibido by viewModel.recibido.collectAsStateWithLifecycle()
    val contexto = LocalContext.current
    val pedido = estado.pedido

    // Si el pedido se eliminó mientras esta pantalla estaba abierta, se vuelve atrás.
    LaunchedEffect(estado) { if (!estado.cargando && pedido == null) alVolver() }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Cobrar")
                        pedido?.let { Text(it.pedido.nombre, style = MaterialTheme.typography.bodyMedium) }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = alVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (pedido != null) {
                        IconButton(onClick = { compartir(contexto, textoCobro(pedido)) }) {
                            Icon(Icons.Default.Share, contentDescription = "Enviar cobro")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (pedido != null && pedido.pedido.estado != EstadoPedido.PAGADO) {
                // El vuelto va en la barra de abajo para que se vea aunque el teclado esté abierto.
                val vuelto = recibido.aCentavos()?.let { it - pedido.pedido.total }
                BarraPagar(vuelto) { viewModel.marcarPagado(alTerminar = alVolver) }
            }
        },
    ) { padding ->
        if (pedido == null) return@Scaffold
        val p = pedido.pedido
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EtiquetaEstado(p.estado, p.paraLlevar)
                if (p.paraLlevar) EtiquetaParaLlevar()
            }
            DetalleCobro(pedido)
            if (p.estado != EstadoPedido.PAGADO) {
                CalculadoraVuelto(p.total, recibido, viewModel::cambiarRecibido)
            }
        }
    }
}

@Composable
private fun DetalleCobro(pedido: PedidoConLineas) {
    val p = pedido.pedido
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            pedido.lineas.sortedBy { it.producto.ordinal }.forEach { linea ->
                FilaDetalle(
                    concepto = "${linea.cantidad} ${linea.producto.nombre}",
                    detalle = if (linea.cantidad > 1) "${linea.precioUnitario.comoDinero()} c/u" else null,
                    monto = (linea.cantidad * linea.precioUnitario).comoDinero(),
                )
            }
            if (p.costoTarrinas > 0) {
                FilaDetalle(
                    concepto = textoTarrinas(p.tarrinasSegundo, p.tarrinasSopa, p.envasesProteina),
                    detalle = null,
                    monto = p.costoTarrinas.comoDinero(),
                )
            }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(p.total.comoDinero(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FilaDetalle(concepto: String, detalle: String?, monto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(concepto, style = MaterialTheme.typography.bodyLarge)
            if (detalle != null) {
                Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(monto, style = MaterialTheme.typography.bodyLarge)
    }
}

// Botones con los montos más comunes y un campo para otro monto.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalculadoraVuelto(total: Int, recibido: String, alCambiar: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val centavos = recibido.aCentavos()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("¿Con cuánto paga?", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf(total) + montosSugeridos(total)).forEach { monto ->
                // El campo muestra el monto sin el "$", porque el "$" ya está fijo a la izquierda.
                val texto = monto.comoMonto()
                FilterChip(
                    selected = centavos == monto,
                    onClick = {
                        focusManager.clearFocus()
                        alCambiar(texto)
                    },
                    label = {
                        Text(if (monto == total) "Exacto" else monto.comoDinero(), style = MaterialTheme.typography.titleMedium)
                    },
                    modifier = Modifier.height(48.dp),
                )
            }
        }
        OutlinedTextField(
            value = recibido,
            onValueChange = alCambiar,
            label = { Text("Monto recibido") },
            prefix = { Text("$") },
            singleLine = true,
            isError = recibido.isNotBlank() && centavos == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Resultado(titulo: String, monto: Int, fondo: Color, colorTexto: Color) {
    Surface(color = fondo, contentColor = colorTexto, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(titulo, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(monto.comoDinero(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

// vuelto = null mientras no se haya escrito un monto válido; negativo si el monto no alcanza.
@Composable
private fun BarraPagar(vuelto: Int?, alPagar: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                vuelto == null -> Unit
                vuelto >= 0 -> Resultado("Vuelto", vuelto, Color(0xFFCDEBCF), Color(0xFF1B5E20))
                else -> Resultado(
                    "Falta",
                    -vuelto,
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Button(onClick = alPagar, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Marcar pagado", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// Abre el menú de "Compartir" de Android (WhatsApp, mensajes...) con el texto de la cuenta.
private fun compartir(contexto: Context, texto: String) {
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
    contexto.startActivity(Intent.createChooser(intent, "Enviar cobro"))
}
