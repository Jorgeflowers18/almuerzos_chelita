package com.chelita.almuerzos.ui.pedidos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chelita.almuerzos.dominio.Cobro
import com.chelita.almuerzos.dominio.EstadoPedido
import com.chelita.almuerzos.dominio.Precios
import com.chelita.almuerzos.dominio.Producto
import com.chelita.almuerzos.dominio.comoDinero
import com.chelita.almuerzos.ui.Contador
import com.chelita.almuerzos.ui.FabricaViewModels

// Pantalla para registrar un pedido nuevo o modificar uno existente.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarPedidoScreen(
    alVolver: () -> Unit,
    viewModel: EditarPedidoViewModel = viewModel(factory = FabricaViewModels),
) {
    val formulario by viewModel.formulario.collectAsStateWithLifecycle()
    val precios by viewModel.precios.collectAsStateWithLifecycle()
    val listo by viewModel.listo.collectAsStateWithLifecycle()
    val cobro = remember(formulario, precios) { formulario.cobro(precios) }

    // Estado propio de la pantalla: qué diálogo está abierto y si ya se intentó guardar
    // (para mostrar los errores solo después del primer intento).
    var confirmarSalida by remember { mutableStateOf(false) }
    var confirmarEliminar by remember { mutableStateOf(false) }
    var intentoGuardar by rememberSaveable { mutableStateOf(false) }

    val intentarVolver = { if (viewModel.hayCambios()) confirmarSalida = true else alVolver() }
    // Captura el botón "atrás" del celular para no perder un pedido a medio escribir.
    BackHandler(onBack = intentarVolver)

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.esNuevo) "Nuevo pedido" else "Editar pedido") },
                navigationIcon = {
                    IconButton(onClick = intentarVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (!viewModel.esNuevo) {
                        IconButton(onClick = { confirmarEliminar = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar pedido")
                        }
                    }
                },
            )
        },
        bottomBar = {
            BarraTotal(cobro, formulario.paraLlevar) {
                intentoGuardar = true
                viewModel.guardar(alTerminar = alVolver)
            }
        },
    ) { padding ->
        if (!listo) return@Scaffold
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CampoNombre(
                nombre = formulario.nombre,
                mostrarError = intentoGuardar && !formulario.nombreValido,
                enfocarAlInicio = viewModel.esNuevo,
                alCambiar = { texto -> viewModel.actualizar { it.copy(nombre = texto) } },
            )

            SelectorLugar(formulario.paraLlevar) { llevar -> viewModel.actualizar { it.copy(paraLlevar = llevar) } }

            Column {
                Text("Productos", style = MaterialTheme.typography.titleMedium)
                Producto.entries.forEach { producto ->
                    FilaProducto(
                        producto = producto,
                        precios = precios,
                        cantidad = formulario.cantidad(producto),
                        alSumar = { viewModel.actualizar { it.sumar(producto) } },
                        alRestar = { viewModel.actualizar { it.restar(producto) } },
                    )
                }
                if (intentoGuardar && !formulario.tieneProductos) {
                    Text(
                        "Agrega al menos un producto",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            OutlinedTextField(
                value = formulario.observaciones,
                onValueChange = { texto -> viewModel.actualizar { it.copy(observaciones = texto) } },
                label = { Text("Observaciones (opcional)") },
                placeholder = { Text("Ej: sin ensalada, poco arroz") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!viewModel.esNuevo) {
                SelectorEstado(formulario.estado, formulario.paraLlevar) { nuevo ->
                    viewModel.actualizar { it.copy(estado = nuevo) }
                }
            }
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Salir sin guardar?") },
            text = { Text("Los cambios de este pedido se van a perder.") },
            confirmButton = { TextButton(onClick = alVolver) { Text("Salir sin guardar") } },
            dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Seguir editando") } },
        )
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text("¿Eliminar este pedido?") },
            text = { Text("El pedido de ${formulario.nombre} se borrará y no se puede recuperar.") },
            confirmButton = {
                TextButton(onClick = { viewModel.eliminar(alTerminar = alVolver) }) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun CampoNombre(nombre: String, mostrarError: Boolean, enfocarAlInicio: Boolean, alCambiar: (String) -> Unit) {
    val foco = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    // En un pedido nuevo, el cursor empieza en el nombre para escribir de inmediato.
    LaunchedEffect(Unit) { if (enfocarAlInicio) foco.requestFocus() }

    OutlinedTextField(
        value = nombre,
        onValueChange = alCambiar,
        label = { Text("Nombre del cliente") },
        singleLine = true,
        isError = mostrarError,
        supportingText = if (mostrarError) ({ Text("Escribe el nombre") }) else null,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        modifier = Modifier.fillMaxWidth().focusRequester(foco),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorLugar(paraLlevar: Boolean, alCambiar: (Boolean) -> Unit) {
    val opciones = listOf(false to "Aquí", true to "Para llevar")
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { indice, (valor, texto) ->
            SegmentedButton(
                selected = paraLlevar == valor,
                onClick = { alCambiar(valor) },
                shape = SegmentedButtonDefaults.itemShape(indice, opciones.size),
                modifier = Modifier.height(48.dp),
            ) {
                Text(texto, style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorEstado(estado: EstadoPedido, paraLlevar: Boolean, alCambiar: (EstadoPedido) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Estado", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            EstadoPedido.entries.forEachIndexed { indice, opcion ->
                SegmentedButton(
                    selected = estado == opcion,
                    onClick = { alCambiar(opcion) },
                    shape = SegmentedButtonDefaults.itemShape(indice, EstadoPedido.entries.size),
                ) {
                    Text(opcion.nombrePara(paraLlevar))
                }
            }
        }
    }
}

@Composable
private fun FilaProducto(producto: Producto, precios: Precios, cantidad: Int, alSumar: () -> Unit, alRestar: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                producto.nombre,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (cantidad > 0) FontWeight.Bold else FontWeight.Normal,
            )
            Text(
                precios.precioDe(producto).comoDinero(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Contador(cantidad, alRestar = alRestar, alSumar = alSumar, descripcion = producto.nombre)
    }
}

// Barra fija abajo: el desglose del cobro y el botón de guardar.
@Composable
private fun BarraTotal(cobro: Cobro, paraLlevar: Boolean, alGuardar: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (paraLlevar && cobro.costoTarrinas > 0) {
                FilaMonto("Platos", cobro.subtotalPlatos.comoDinero())
                FilaMonto(detalleTarrinas(cobro), cobro.costoTarrinas.comoDinero())
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(
                    cobro.total.comoDinero(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            Button(onClick = alGuardar, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text("Guardar pedido", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun FilaMonto(concepto: String, monto: String) {
    Row {
        Text(concepto, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(monto, style = MaterialTheme.typography.bodyMedium)
    }
}

// "Tarrinas (2 segundo, 3 sopa, 1 proteína)"
private fun detalleTarrinas(cobro: Cobro): String {
    val partes = buildList {
        if (cobro.tarrinasSegundo > 0) add("${cobro.tarrinasSegundo} segundo")
        if (cobro.tarrinasSopa > 0) add("${cobro.tarrinasSopa} sopa")
        if (cobro.envasesProteina > 0) add("${cobro.envasesProteina} proteína")
    }
    return "Tarrinas (${partes.joinToString(", ")})"
}
