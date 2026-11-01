package com.chelita.almuerzos.dominio

// Un "enum" es una lista cerrada de opciones: los productos que se pueden pedir.
// Cada opción guarda el nombre que se muestra en pantalla.
enum class Producto(val nombre: String) {
    ALMUERZO("Almuerzo"),
    ALMUERZO_MIXTO("Almuerzo mixto"),
    SEGUNDO("Segundo"),
    SEGUNDO_MIXTO("Segundo mixto"),
    SOPA("Sopa"),
    PORCION_PROTEINA("Porción de proteína");

    // ¿Este producto incluye un segundo? (necesita tarrina de segundo para llevar)
    val llevaSegundo: Boolean
        get() = this == ALMUERZO || this == ALMUERZO_MIXTO || this == SEGUNDO || this == SEGUNDO_MIXTO

    // ¿Este producto incluye una sopa? (necesita tarrina pequeña para llevar)
    val llevaSopa: Boolean
        get() = this == ALMUERZO || this == ALMUERZO_MIXTO || this == SOPA

    // ¿Lleva proteína extra dentro del mismo plato?
    val esMixto: Boolean
        get() = this == ALMUERZO_MIXTO || this == SEGUNDO_MIXTO

    // El almuerzo y el segundo incluyen bebida sin costo.
    val llevaBebida: Boolean
        get() = llevaSegundo
}
