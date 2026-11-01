package com.chelita.almuerzos.dominio

// "Función de extensión": agrega un método nuevo a Int, para escribir 250.comoDinero() -> "$2.50".
fun Int.comoDinero(): String {
    val dolares = this / 100
    val centavos = (this % 100).toString().padStart(2, '0')
    return "$$dolares.$centavos"
}
