package com.chelita.almuerzos.ui.resumen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chelita.almuerzos.dominio.Periodo
import com.chelita.almuerzos.dominio.Rango
import com.chelita.almuerzos.dominio.comoDinero
import com.chelita.almuerzos.ui.FabricaViewModels
import com.chelita.almuerzos.ui.diaDelMes
import com.chelita.almuerzos.ui.fechaLarga
import com.chelita.almuerzos.ui.mesYAnio
import com.chelita.almuerzos.ui.textoTarrinas

// Pantalla de resumen: cuánto se vendió hoy, este mes o el mes anterior.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenScreen(viewModel: ResumenViewModel = viewModel(factory = FabricaViewModels)) {
    val periodo by viewModel.periodo.collectAsStateWithLifecycle()
    val estado by viewModel.estado.collectAsStateWithLifecycle()
    val resumen = estado.resumen

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Resumen")
                        estado.rango?.let { Text(textoRango(periodo, it), style = MaterialTheme.typography.bodyMedium) }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { SelectorPeriodo(periodo, viewModel::elegirPeriodo) }

            if (estado.cargando) return@LazyColumn
            if (resumen.pedidos == 0) {
                item {
                    Text(
                        "No hay pedidos en este periodo.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                    )
                }
                return@LazyColumn
            }

            item { TotalVendido(resumen) }
            item { CobradoYPorCobrar(resumen) }
            item { DetalleProductos(resumen) }
            // El "día por día" solo tiene sentido cuando el periodo es un mes.
            if (periodo != Periodo.HOY) {
                item { DiaPorDia(resumen) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorPeriodo(seleccionado: Periodo, alElegir: (Periodo) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        Periodo.entries.forEachIndexed { indice, opcion ->
            SegmentedButton(
                selected = opcion == seleccionado,
                onClick = { alElegir(opcion) },
                shape = SegmentedButtonDefaults.itemShape(indice, Periodo.entries.size),
            ) {
                Text(opcion.etiqueta)
            }
        }
    }
}

@Composable
private fun TotalVendido(resumen: Resumen) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Total vendido", style = MaterialTheme.typography.titleMedium)
            Text(resumen.total.comoDinero(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Text(
                "${cantidad(resumen.pedidos, "pedido", "pedidos")} · ${resumen.paraLlevar} para llevar",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun CobradoYPorCobrar(resumen: Resumen) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Casilla(
            titulo = "Cobrado",
            monto = resumen.cobrado,
            detalle = null,
            fondo = Color(0xFFCDEBCF),
            colorTexto = Color(0xFF1B5E20),
            modifier = Modifier.weight(1f),
        )
        // "Por cobrar" se pinta en rojo solo si hay algo pendiente, para que llame la atención.
        val hayDeuda = resumen.porCobrar > 0
        Casilla(
            titulo = "Por cobrar",
            monto = resumen.porCobrar,
            detalle = if (hayDeuda) cantidad(resumen.pedidosPorCobrar, "pedido", "pedidos") else null,
            fondo = if (hayDeuda) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainer,
            colorTexto = if (hayDeuda) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Casilla(titulo: String, monto: Int, detalle: String?, fondo: Color, colorTexto: Color, modifier: Modifier) {
    Surface(color = fondo, contentColor = colorTexto, shape = RoundedCornerShape(16.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(monto.comoDinero(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            // Siempre ocupa su línea, para que las dos casillas midan lo mismo.
            Text(detalle ?: " ", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun DetalleProductos(resumen: Resumen) {
    Tarjeta("Qué se vendió") {
        resumen.productos.forEach { venta ->
            Fila("${venta.cantidad} ${venta.producto.nombre}", venta.monto.comoDinero())
        }
        if (resumen.costoTarrinas > 0) {
            Fila(
                textoTarrinas(resumen.tarrinasSegundo, resumen.tarrinasSopa, resumen.envasesProteina),
                resumen.costoTarrinas.comoDinero(),
            )
        }
        HorizontalDivider()
        Fila("Total", resumen.total.comoDinero(), negrita = true)
    }
}

@Composable
private fun DiaPorDia(resumen: Resumen) {
    Tarjeta("Día por día") {
        resumen.dias.forEach { dia ->
            Fila(
                "${diaDelMes(dia.fecha)} · ${cantidad(dia.pedidos, "pedido", "pedidos")}",
                dia.total.comoDinero(),
            )
        }
    }
}

@Composable
private fun Tarjeta(titulo: String, contenido: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            contenido()
        }
    }
}

@Composable
private fun Fila(concepto: String, monto: String, negrita: Boolean = false) {
    val peso = if (negrita) FontWeight.Bold else FontWeight.Normal
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(concepto, style = MaterialTheme.typography.bodyLarge, fontWeight = peso, modifier = Modifier.weight(1f))
        Text(monto, style = MaterialTheme.typography.bodyLarge, fontWeight = peso)
    }
}

// "Domingo 1 de noviembre" para hoy; "Noviembre 2026" para un mes.
private fun textoRango(periodo: Periodo, rango: Rango): String =
    if (periodo == Periodo.HOY) fechaLarga(rango.desde) else mesYAnio(rango.desde)

// "1 pedido", "3 pedidos"
private fun cantidad(numero: Int, singular: String, plural: String) = "$numero ${if (numero == 1) singular else plural}"
