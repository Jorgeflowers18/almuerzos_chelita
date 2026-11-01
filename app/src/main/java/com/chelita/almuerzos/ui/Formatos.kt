package com.chelita.almuerzos.ui

import com.chelita.almuerzos.datos.PedidoConLineas
import com.chelita.almuerzos.dominio.comoDinero
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Textos que se repiten en varias pantallas.

// "2 Almuerzo · 1 Sopa", en el orden del menú.
fun resumenProductos(pedido: PedidoConLineas): String =
    pedido.lineas
        .sortedBy { it.producto.ordinal }
        .joinToString(" · ") { "${it.cantidad} ${it.producto.nombre}" }

// "Tarrinas (2 segundo, 3 sopa, 1 proteína)"
fun textoTarrinas(segundo: Int, sopa: Int, proteina: Int): String {
    val partes = buildList {
        if (segundo > 0) add("$segundo segundo")
        if (sopa > 0) add("$sopa sopa")
        if (proteina > 0) add("$proteina proteína")
    }
    return "Tarrinas (${partes.joinToString(", ")})"
}

private val espanol = Locale.forLanguageTag("es")

// "Domingo 1 de noviembre"
fun fechaLarga(fecha: LocalDate): String =
    fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", espanol)).replaceFirstChar { it.uppercase() }

// "Domingo 1", para listas donde el mes ya se sabe.
fun diaDelMes(fecha: LocalDate): String =
    fecha.format(DateTimeFormatter.ofPattern("EEEE d", espanol)).replaceFirstChar { it.uppercase() }

// "Noviembre 2026"
fun mesYAnio(fecha: LocalDate): String =
    fecha.format(DateTimeFormatter.ofPattern("MMMM yyyy", espanol)).replaceFirstChar { it.uppercase() }

// La hora en que se tomó el pedido: "12:35"
fun horaDe(milisegundos: Long, zona: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(milisegundos).atZone(zona).format(DateTimeFormatter.ofPattern("HH:mm"))

// Cuándo pasó algo, en palabras: "hoy a las 13:45", "ayer a las 9:05", "el 28 de octubre a las 18:30"
fun momentoDe(milisegundos: Long, zona: ZoneId = ZoneId.systemDefault()): String {
    val momento = Instant.ofEpochMilli(milisegundos).atZone(zona)
    val hoy = LocalDate.now(zona)
    val dia = when (momento.toLocalDate()) {
        hoy -> "hoy"
        hoy.minusDays(1) -> "ayer"
        else -> "el " + momento.format(DateTimeFormatter.ofPattern("d 'de' MMMM", espanol))
    }
    return "$dia a las ${momento.format(DateTimeFormatter.ofPattern("H:mm"))}"
}

// La cuenta lista para mandarla por WhatsApp. Los asteriscos ponen el texto en negrita.
fun textoCobro(pedido: PedidoConLineas): String {
    val p = pedido.pedido
    return buildString {
        appendLine("*Almuerzos Chelita*")
        appendLine("Pedido de ${p.nombre}" + if (p.paraLlevar) " (para llevar)" else "")
        pedido.lineas.sortedBy { it.producto.ordinal }.forEach {
            appendLine("${it.cantidad} ${it.producto.nombre}: ${(it.cantidad * it.precioUnitario).comoDinero()}")
        }
        if (p.costoTarrinas > 0) {
            appendLine("${textoTarrinas(p.tarrinasSegundo, p.tarrinasSopa, p.envasesProteina)}: ${p.costoTarrinas.comoDinero()}")
        }
        append("*Total: ${p.total.comoDinero()}*")
    }
}
