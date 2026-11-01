package com.chelita.almuerzos.dominio

// "Función de extensión": agrega un método nuevo a Int, para escribir 250.comoDinero() -> "$2.50".
fun Int.comoDinero(): String {
    val dolares = this / 100
    val centavos = (this % 100).toString().padStart(2, '0')
    return "$$dolares.$centavos"
}

// El monto sin el "$", para los campos de texto que ya muestran el "$" fijo: 250 -> "2.50"
fun Int.comoMonto(): String = comoDinero().removePrefix("$")

// Convierte lo que escribe la mesera ("10", "7.5", "7,50", "$8") a centavos.
// Devuelve null si no es un monto válido.
fun String.aCentavos(): Int? {
    val partes = Regex("""[$]?\s*(\d{1,5})(?:[.,](\d{0,2}))?""").matchEntire(trim()) ?: return null
    val dolares = partes.groupValues[1].toInt()
    val centavos = partes.groupValues[2].padEnd(2, '0').toInt()
    return dolares * 100 + centavos
}

// Montos con los que probablemente paguen un total: el siguiente dólar y los billetes de 5, 10 y 20
// (redondeados hacia arriba si el total es mayor). Siempre mayores que el total.
fun montosSugeridos(total: Int): List<Int> {
    if (total <= 0) return emptyList()
    return listOf(100, 500, 1000, 2000)
        .map { billete -> (total + billete - 1) / billete * billete }
        .filter { it > total }
        .distinct()
}
