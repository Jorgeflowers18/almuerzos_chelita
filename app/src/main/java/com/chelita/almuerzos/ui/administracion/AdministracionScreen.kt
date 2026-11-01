package com.chelita.almuerzos.ui.administracion

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chelita.almuerzos.datos.respaldo.EstadoRespaldos
import com.chelita.almuerzos.datos.respaldo.TipoRespaldo
import com.chelita.almuerzos.dominio.Precios
import com.chelita.almuerzos.dominio.Producto
import com.chelita.almuerzos.dominio.comoDinero
import com.chelita.almuerzos.ui.FabricaViewModels
import com.chelita.almuerzos.ui.fechaLarga
import com.chelita.almuerzos.ui.momentoDe
import java.time.LocalDate

// Pantalla de administración: menú de hoy, precios y limpieza de pedidos antiguos.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdministracionScreen(viewModel: AdministracionViewModel = viewModel(factory = FabricaViewModels)) {
    val menu by viewModel.menu.collectAsStateWithLifecycle()
    val menuGuardado by viewModel.menuGuardado.collectAsStateWithLifecycle()
    val precios by viewModel.precios.collectAsStateWithLifecycle()
    val preciosGuardados by viewModel.preciosGuardados.collectAsStateWithLifecycle()
    val antiguos by viewModel.pedidosAntiguos.collectAsStateWithLifecycle()
    val limite by viewModel.limiteDeLimpieza.collectAsStateWithLifecycle()
    val respaldo by viewModel.respaldo.collectAsStateWithLifecycle()
    val propuesta by viewModel.propuesta.collectAsStateWithLifecycle()
    val contexto = LocalContext.current

    // Selectores de Android: uno para elegir la carpeta de respaldos y otro para elegir
    // el archivo a restaurar. Devuelven null si la mesera cancela.
    val elegirCarpeta = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let(viewModel::elegirCarpeta)
    }
    val elegirArchivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::prepararRestauracion)
    }
    LaunchedEffect(Unit) { viewModel.envios.collect { compartirArchivo(contexto, it) } }

    val avisos = remember { SnackbarHostState() }
    var confirmarBorrado by remember { mutableStateOf(false) }
    // Al guardar se cierra el teclado, para que se vea el aviso y la barra de abajo.
    val focusManager = LocalFocusManager.current
    val guardarMenu = {
        focusManager.clearFocus()
        viewModel.guardarMenu()
    }
    val guardarPrecios = {
        focusManager.clearFocus()
        viewModel.guardarPrecios()
    }

    // Muestra abajo cada aviso que manda el ViewModel.
    LaunchedEffect(Unit) { viewModel.mensajes.collect { avisos.showSnackbar(it) } }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = { TopAppBar(title = { Text("Administración") }) },
        snackbarHost = { SnackbarHost(avisos) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SeccionMenu(menu, hayCambios = menu != menuGuardado, viewModel::cambiarMenu, guardarMenu)

            preciosGuardados?.let { guardados ->
                SeccionPrecios(
                    formulario = precios,
                    guardados = guardados,
                    alCambiarPrecio = viewModel::cambiarPrecio,
                    alCambiarPorciones = viewModel::cambiarPorciones,
                    alGuardar = guardarPrecios,
                )
            }

            SeccionRespaldos(
                estado = respaldo,
                alElegirCarpeta = { elegirCarpeta.launch(null) },
                alRespaldar = viewModel::respaldarAhora,
                alEnviar = viewModel::enviarRespaldo,
                // "*/*": WhatsApp a veces guarda el archivo sin decir que es JSON; el contenido se revisa al leerlo.
                alRestaurar = { elegirArchivo.launch(arrayOf("*/*")) },
            )

            limite?.let { SeccionLimpieza(antiguos, it, alBorrar = { confirmarBorrado = true }) }
        }
    }

    propuesta?.let { DialogoRestaurar(it, viewModel::confirmarRestauracion, viewModel::cancelarRestauracion) }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Borrar pedidos antiguos?") },
            text = {
                Text(
                    "Antes de borrar se guardarán todos los datos en el archivo " +
                        "\"${TipoRespaldo.ANTES_DE_LIMPIAR.archivo}\" de la carpeta de respaldos " +
                        "(reemplaza al de la limpieza anterior). Luego se borrará" +
                        if (antiguos == 1) " 1 pedido de este celular." else "n $antiguos pedidos de este celular.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarBorrado = false
                    viewModel.borrarAntiguos()
                }) {
                    Text("Borrar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SeccionMenu(
    menu: FormularioMenu,
    hayCambios: Boolean,
    alCambiar: ((FormularioMenu) -> FormularioMenu) -> Unit,
    alGuardar: () -> Unit,
) {
    Seccion("Menú de hoy", "Aparece arriba en la lista de pedidos.") {
        OutlinedTextField(
            value = menu.segundo,
            onValueChange = { texto -> alCambiar { it.copy(segundo = texto) } },
            label = { Text("Segundo") },
            placeholder = { Text("Ej: Seco de pollo") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = menu.sopa,
            onValueChange = { texto -> alCambiar { it.copy(sopa = texto) } },
            label = { Text("Sopa") },
            placeholder = { Text("Ej: Locro de papa") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = alGuardar, enabled = hayCambios, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("Guardar menú")
        }
    }
}

@Composable
private fun SeccionPrecios(
    formulario: FormularioPrecios,
    guardados: Precios,
    alCambiarPrecio: (CampoPrecio, String) -> Unit,
    alCambiarPorciones: (String) -> Unit,
    alGuardar: () -> Unit,
) {
    val nuevos = formulario.aPrecios()

    Seccion("Precios", "Los pedidos ya registrados conservan el precio con que se tomaron.") {
        Text("Platos", style = MaterialTheme.typography.titleSmall)
        ParDeCampos(formulario, CampoPrecio.ALMUERZO, CampoPrecio.SEGUNDO, alCambiarPrecio)
        ParDeCampos(formulario, CampoPrecio.SOPA, CampoPrecio.PROTEINA_EXTRA, alCambiarPrecio)
        // Los mixtos no se escriben: son el plato más la proteína extra.
        nuevos?.let {
            Text(
                "${Producto.ALMUERZO_MIXTO.nombre}: ${it.precioDe(Producto.ALMUERZO_MIXTO).comoDinero()} · " +
                    "${Producto.SEGUNDO_MIXTO.nombre}: ${it.precioDe(Producto.SEGUNDO_MIXTO).comoDinero()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text("Tarrinas para llevar", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        ParDeCampos(formulario, CampoPrecio.TARRINA_SEGUNDO, CampoPrecio.TARRINA_SOPA, alCambiarPrecio)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoMonto(formulario, CampoPrecio.ENVASE_PROTEINA, alCambiarPrecio, Modifier.weight(1f))
            OutlinedTextField(
                value = formulario.porcionesPorEnvase,
                onValueChange = alCambiarPorciones,
                label = { Text("Porciones por envase") },
                singleLine = true,
                isError = !formulario.porcionesValidas,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f),
            )
        }

        Button(
            onClick = alGuardar,
            enabled = nuevos != null && nuevos != guardados,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("Guardar precios")
        }
    }
}

@Composable
private fun ParDeCampos(
    formulario: FormularioPrecios,
    izquierdo: CampoPrecio,
    derecho: CampoPrecio,
    alCambiar: (CampoPrecio, String) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CampoMonto(formulario, izquierdo, alCambiar, Modifier.weight(1f))
        CampoMonto(formulario, derecho, alCambiar, Modifier.weight(1f))
    }
}

@Composable
private fun CampoMonto(
    formulario: FormularioPrecios,
    campo: CampoPrecio,
    alCambiar: (CampoPrecio, String) -> Unit,
    modifier: Modifier,
) {
    OutlinedTextField(
        value = formulario.monto(campo),
        onValueChange = { alCambiar(campo, it) },
        label = { Text(campo.etiqueta) },
        prefix = { Text("$") },
        singleLine = true,
        isError = !formulario.montoValido(campo),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}

@Composable
private fun SeccionRespaldos(
    estado: EstadoRespaldos,
    alElegirCarpeta: () -> Unit,
    alRespaldar: () -> Unit,
    alEnviar: () -> Unit,
    alRestaurar: () -> Unit,
) {
    Seccion(
        "Respaldos",
        "Se guarda un solo archivo que se reemplaza cada vez: al salir de la app (como mucho cada " +
            "15 minutos) y al tocar \"Respaldar ahora\". Queda fuera de la app, así que no se pierde " +
            "aunque se desinstale.",
    ) {
        if (estado.carpeta == null) {
            Text(
                "Todavía no hay carpeta de respaldos: los datos no se están respaldando. " +
                    "Elige una, por ejemplo una carpeta nueva \"Respaldos Chelita\" dentro de Documentos.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            Button(onClick = alElegirCarpeta, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("Elegir carpeta de respaldos")
            }
        } else {
            Column {
                Text("Carpeta: ${estado.carpeta}", style = MaterialTheme.typography.bodyLarge)
                Text(
                    estado.ultimoAutomatico?.let { "Último respaldo: ${momentoDe(it)}" } ?: "Todavía no hay respaldo",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                )
                estado.ultimoAntesDeLimpiar?.let {
                    Text(
                        "Respaldo antes de limpiar: ${momentoDe(it)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(onClick = alRespaldar, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text("Respaldar ahora")
            }
            TextButton(onClick = alElegirCarpeta, modifier = Modifier.fillMaxWidth()) {
                Text("Cambiar carpeta")
            }
        }

        HorizontalDivider()
        Text("Pasar los datos a otro celular", style = MaterialTheme.typography.titleSmall)
        Text(
            "Envía el respaldo por WhatsApp, Drive o correo. En el otro celular, guarda el archivo " +
                "y tócalo desde \"Restaurar desde archivo\".",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = alEnviar, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("Enviar respaldo")
        }
        OutlinedButton(onClick = alRestaurar, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("Restaurar desde archivo")
        }
    }
}

@Composable
private fun DialogoRestaurar(propuesta: PropuestaRestauracion, alConfirmar: () -> Unit, alCancelar: () -> Unit) {
    val pedidos = propuesta.contenido.pedidos
    val fechas = pedidos.map { it.pedido.fecha }
    val rango = when {
        fechas.isEmpty() -> ""
        fechas.min() == fechas.max() -> " (del ${fechaCorta(fechas.min())})"
        else -> " (del ${fechaCorta(fechas.min())} al ${fechaCorta(fechas.max())})"
    }
    AlertDialog(
        onDismissRequest = alCancelar,
        title = { Text("¿Restaurar este respaldo?") },
        text = {
            Text(
                "Respaldo guardado ${momentoDe(propuesta.contenido.creadoEn)}, con " +
                    (if (pedidos.size == 1) "1 pedido" else "${pedidos.size} pedidos") + "$rango.\n\n" +
                    if (propuesta.pedidosActuales > 0) {
                        "Lo que hay ahora en este celular (${propuesta.pedidosActuales} " +
                            (if (propuesta.pedidosActuales == 1) "pedido" else "pedidos") +
                            ") se reemplazará por los datos del respaldo."
                    } else {
                        "Este celular no tiene pedidos todavía."
                    },
            )
        },
        confirmButton = { TextButton(onClick = alConfirmar) { Text("Restaurar") } },
        dismissButton = { TextButton(onClick = alCancelar) { Text("Cancelar") } },
    )
}

// "1 de noviembre"
private fun fechaCorta(fecha: LocalDate): String = fechaLarga(fecha).substringAfter(" ")

// Abre el menú de "Compartir" de Android con el archivo de respaldo.
private fun compartirArchivo(contexto: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("application/json")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_SUBJECT, "Respaldo Almuerzos Chelita")
        // Da permiso a la app elegida (WhatsApp, Drive...) para leer el archivo.
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    contexto.startActivity(Intent.createChooser(intent, "Enviar respaldo"))
}

@Composable
private fun SeccionLimpieza(antiguos: Int, limite: LocalDate, alBorrar: () -> Unit) {
    Seccion(
        "Pedidos antiguos",
        "Se conservan los pedidos de los últimos 2 meses. Los de antes del " +
            "${fechaLarga(limite).replaceFirstChar { it.lowercase() }} se pueden borrar.",
    ) {
        Text(
            when (antiguos) {
                0 -> "No hay pedidos antiguos."
                1 -> "Hay 1 pedido antiguo."
                else -> "Hay $antiguos pedidos antiguos."
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedButton(
            onClick = alBorrar,
            enabled = antiguos > 0,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text("Borrar pedidos antiguos")
        }
    }
}

@Composable
private fun Seccion(titulo: String, explicacion: String, contenido: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(explicacion, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            contenido()
        }
    }
}
