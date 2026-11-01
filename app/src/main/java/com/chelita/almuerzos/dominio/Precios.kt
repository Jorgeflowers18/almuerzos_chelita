package com.chelita.almuerzos.dominio

// Precios vigentes, todos en CENTAVOS para evitar errores de redondeo ($2.50 = 250).
// Los valores después del "=" son los precios por defecto; en la fase 7 se podrán
// editar desde la pantalla de Administración.
// Un "data class" es una clase que solo guarda datos (como un registro).
data class Precios(
    val almuerzo: Int = 250,
    val segundo: Int = 200,
    val sopa: Int = 50,
    val proteinaExtra: Int = 75,
    val tarrinaSegundo: Int = 25,
    val tarrinaSopa: Int = 15,
    val envaseProteina: Int = 15,
    val porcionesPorEnvase: Int = 3,
) {
    // "init" se ejecuta al crear el objeto: aquí validamos que los datos tengan sentido.
    init {
        require(porcionesPorEnvase >= 1) { "Debe caber al menos 1 porción por envase" }
    }

    // El mixto no tiene precio propio: es el plato base más la proteína extra.
    // Así, si sube la proteína, el mixto se actualiza solo.
    fun precioDe(producto: Producto): Int = when (producto) {
        Producto.ALMUERZO -> almuerzo
        Producto.ALMUERZO_MIXTO -> almuerzo + proteinaExtra
        Producto.SEGUNDO -> segundo
        Producto.SEGUNDO_MIXTO -> segundo + proteinaExtra
        Producto.SOPA -> sopa
        Producto.PORCION_PROTEINA -> proteinaExtra
    }
}
