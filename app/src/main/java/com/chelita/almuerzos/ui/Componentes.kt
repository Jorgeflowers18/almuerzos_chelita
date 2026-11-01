package com.chelita.almuerzos.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chelita.almuerzos.dominio.EstadoPedido

// Piezas de interfaz que se reutilizan en varias pantallas.

// Una "pastilla" de color con texto, por ejemplo "Para llevar".
@Composable
fun Etiqueta(texto: String, fondo: Color, colorTexto: Color, modifier: Modifier = Modifier) {
    Surface(color = fondo, contentColor = colorTexto, shape = RoundedCornerShape(50), modifier = modifier) {
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

// Etiqueta del estado con un color distinto para cada uno.
@Composable
fun EtiquetaEstado(estado: EstadoPedido, paraLlevar: Boolean) {
    val (fondo, texto) = when (estado) {
        EstadoPedido.PENDIENTE -> Color(0xFFFFE0B2) to Color(0xFF8A3B00)
        EstadoPedido.SERVIDO -> Color(0xFFD6E6FB) to Color(0xFF0D3F80)
        EstadoPedido.PAGADO -> Color(0xFFCDEBCF) to Color(0xFF1B5E20)
    }
    Etiqueta(estado.nombrePara(paraLlevar), fondo, texto)
}

// Botones grandes de − y + con la cantidad en medio. Pensados para tocar rápido con el pulgar.
@Composable
fun Contador(cantidad: Int, alRestar: () -> Unit, alSumar: () -> Unit, descripcion: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BotonRedondo("−", "Quitar $descripcion", habilitado = cantidad > 0, alTocar = alRestar)
        Text(
            "$cantidad",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = if (cantidad > 0) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(48.dp),
        )
        BotonRedondo("+", "Agregar $descripcion", habilitado = true, alTocar = alSumar)
    }
}

@Composable
private fun BotonRedondo(simbolo: String, descripcion: String, habilitado: Boolean, alTocar: () -> Unit) {
    FilledTonalButton(
        onClick = alTocar,
        enabled = habilitado,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        // La descripción ("Agregar Almuerzo") la leen los lectores de pantalla y las pruebas.
        modifier = Modifier.size(52.dp).semantics { contentDescription = descripcion },
    ) {
        Text(simbolo, style = MaterialTheme.typography.headlineSmall)
    }
}
