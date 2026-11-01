package com.chelita.almuerzos.dominio

// Estados por los que pasa un pedido, en orden.
// En la base de datos se guarda el nombre en mayúsculas ("PENDIENTE"), así que
// NO renombrar estas opciones una vez que la app esté en uso.
enum class EstadoPedido(val nombre: String) {
    PENDIENTE("Pendiente"),
    SERVIDO("Servido"),
    PAGADO("Pagado");

    // El estado al que se avanza con el botón de la lista (null = ya no hay siguiente).
    val siguiente: EstadoPedido?
        get() = when (this) {
            PENDIENTE -> SERVIDO
            SERVIDO -> PAGADO
            PAGADO -> null
        }

    // Para los pedidos para llevar, "Servido" se muestra como "Entregado".
    fun nombrePara(paraLlevar: Boolean): String =
        if (this == SERVIDO && paraLlevar) "Entregado" else nombre
}
